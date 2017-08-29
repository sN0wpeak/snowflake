package io.shuidi.snowflake.server.server;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator;
import io.shuidi.snowflake.core.util.http.HttpClientUtil;
import io.shuidi.snowflake.core.util.http.impl.HttpResponseCallbackHandlerJsonHandler;
import io.shuidi.snowflake.server.config.SnowflakeConfig;
import io.shuidi.snowflake.server.enums.ErrorCode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.CancelLeadershipException;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.http.client.methods.HttpGet;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator.WORKER_ID_MAX_VALUE;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 16:36
 */

@Service
public class SnowflakeServer implements LeaderSelectorListener, InitializingBean, AutoCloseable, Watcher {
	private static Logger LOGGER = LoggerFactory.getLogger(SnowflakeServer.class);

	private static final byte[] NONE = new byte[]{};

	@Autowired
	SnowflakeConfig snowflakeConfig;
	@Autowired
	private CuratorFramework zkClient;
	private LeaderSelector leaderSelector;
	private volatile boolean isLeader = false;
	public static final Set<Integer> ALL_WORKER_IDS = ImmutableSortedSet.copyOf(Stream.iterate(0, a -> ++a).limit(1024).iterator());

	private int workerId;

	public void start() throws Exception {
		LOGGER.info("start SnowflakeServer... ip: {}", getHostname());
		zkClient.start();
		leaderSelector.start();
		while (!leaderSelector.hasLeadership()) {
			Thread.sleep(1000);
		}
		registerWorkerId(fetchWorkerId());
	}


	public boolean isLeader() {
		return isLeader;
	}

	public synchronized int allocWorkerId() throws Exception {
		Set<Integer> diff = Sets.difference(ALL_WORKER_IDS, peers().keySet());
		Optional<Integer> optional = diff.stream().findFirst();
		return optional.isPresent() ? optional.get() : ErrorCode.NOT_MORE_WORKER_ID.getCode();
	}


	private int fetchWorkerId() throws Exception {
		if (isLeader()) {
			return allocWorkerId();
		} else {
			String path = "/api/snowflake/get-alloc-workerid";
			String url = "http://" + leaderSelector.getLeader().getId() + ":" + snowflakeConfig.getPort() + path;
			int tries = 0;
			while (true) {
				try {
					JSONObject result =
							HttpClientUtil.execute(new HttpGet(url), new HttpResponseCallbackHandlerJsonHandler<>(JSONObject.class));
					if (result.getInteger("code") == 0) {
						return result.getInteger("data").getInteger("workerId");
					} else {
						LOGGER.error("call fetchWorkerId error {}", result.toJSONString());
						throw new IllegalStateException(result.getString("msg"));
					}
				} catch (IOException | HttpServerErrorException e) {
					if (tries++ > 2) {
						throw e;
					} else {
						tries++;
						Thread.sleep(1000);
					}
				}
			}
		}
	}


	public void registerWorkerId(int workerId) throws Exception {
		Preconditions.checkArgument(workerId >= 0L && workerId < WORKER_ID_MAX_VALUE);
		int tries = 0;
		while (true) {
			try {
				zkClient.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
				        .forPath("%s/%s".format(snowflakeConfig.getWorkerIdZkPath(), workerId + ""),
				                 (getHostname() + ":" + snowflakeConfig.getPort()).getBytes());
				SnowflakeIDGenerator.setWorkerId(workerId);
				this.workerId = workerId;
				return;
			} catch (Exception e) {
				LOGGER.error("registerWorkerId", e);
				if (e instanceof KeeperException.NodeExistsException) {
					if (tries > 2) {
						throw e;
					}
				} else {
					tries++;
					Thread.sleep(1000);
				}
			}
		}
	}

	private String getHostname() throws UnknownHostException {return InetAddress.getLocalHost().getHostAddress();}

	private void sanityCheckPeers() throws Exception {
		int peerCount = 0;
		ImmutableMap<Integer, Peer> peerImmutableMap = peers();
//		Map<Integer, Peer> peerMap = Maps.newHashMap(peerImmutableMap);
//		peerImmutableMap.entrySet().stream()
//		                .filter(peer ->
//				                        !peer.getValue().getHost().equals(snowflakeConfig.getServer()) &&
//				                        peer.getValue().getPort() != snowflakeConfig.getPort()
//		                ).map(integerPeerEntry -> {
//
//		});

	}

	public ImmutableMap<Integer, Peer> peers() throws Exception {
		/**
		 * 如果
		 */
		ImmutableMap.Builder<Integer, Peer> peerBuilder = ImmutableMap.builder();
		try {
			zkClient.getData().forPath(snowflakeConfig.getWorkerIdZkPath());
		} catch (Exception e) {
			zkClient.create().withMode(CreateMode.PERSISTENT).forPath(snowflakeConfig.getWorkerIdZkPath(), NONE);
		}
		List<String> children = zkClient.getChildren().forPath(snowflakeConfig.getWorkerIdZkPath());
		for (String child : children) {
			String chaildPath = "%s/%s".format(snowflakeConfig.getWorkerIdZkPath(), child);
			LOGGER.info(chaildPath);
			String peer = new String(zkClient.getData().forPath(chaildPath));
			if (StringUtils.isEmpty(peer)) {
				continue;
			}
			String[] list = peer.split(":");
			peerBuilder.put(Integer.valueOf(list[1]), new Peer(list[0], Integer.valueOf(list[0])));
		}
		ImmutableMap<Integer, Peer> peerImmutableMap = peerBuilder.build();
		LOGGER.info("found {} children", peerImmutableMap.size());
		return peerImmutableMap;
	}

	@Override
	public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
		isLeader = true;
		LOGGER.info("I'm leader {}", getHostname());
		zkClient.getChildren().usingWatcher(this).forPath(snowflakeConfig.getWorkerIdZkPath());
		while (isLeader) {
			try {
				if (!snowflakeConfig.isSkipSanityChecks()) {
					sanityCheckPeers();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Thread.sleep(30000);
		}

	}

	@Override
	public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
		if (curatorFramework.getConnectionStateErrorPolicy().isErrorState(connectionState)) {
			reset();
			throw new CancelLeadershipException();
		}
	}

	private void reset() {
		isLeader = false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		leaderSelector = new LeaderSelector(zkClient, snowflakeConfig.getLeaderPath(), this);
		leaderSelector.autoRequeue();
	}

	@Override
	public void close() throws Exception {
		leaderSelector.close();
	}

	public int getWorkerId() {
		return workerId;
	}

	@Override
	public void process(WatchedEvent watchedEvent) {
		if (isLeader()) {
			LOGGER.info("", watchedEvent);
			if (watchedEvent.getState() == Event.KeeperState.Disconnected) {

			} else if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {

			}
		}
	}

	public static class Peer {
		private String host;
		private int port;

		public String getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public Peer(String host, int port) {

			this.host = host;
			this.port = port;
		}
	}

}

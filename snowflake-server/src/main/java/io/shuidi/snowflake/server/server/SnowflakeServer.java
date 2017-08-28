package io.shuidi.snowflake.server.server;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator;
import io.shuidi.snowflake.core.util.http.HttpClientUtil;
import io.shuidi.snowflake.core.util.http.impl.HttpResponseCallbackHandlerJsonHandler;
import io.shuidi.snowflake.server.config.SnowflakeConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.CancelLeadershipException;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.http.client.methods.HttpGet;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 16:36
 */

@Service
public class SnowflakeServer implements LeaderSelectorListener, InitializingBean, AutoCloseable {
	private static Logger LOGGER = LoggerFactory.getLogger(SnowflakeServer.class);

	private static final byte[] NONE = new byte[]{};

	@Autowired
	SnowflakeConfig snowflakeConfig;
	@Autowired
	ZkClient zkClient;
	private CuratorFramework client;
	private LeaderSelector leaderSelector;
	private volatile boolean isLeader = false;
	private volatile CountDownLatch leaderLatch;

	private String leaderPath = "snowleader";

	public void start() throws Exception {
		LOGGER.info("start SnowflakeServer... ip: {}", getHostname());
		leaderSelector.start();
		while (!leaderSelector.hasLeadership()) {
			Thread.sleep(1000);
		}
		registerWorkerId(fetchWorkerId());
	}

	public boolean isLeader() {
		return isLeader;
	}

	private int fetchWorkerId() throws Exception {
		String path = "/api/snowflake/get-fetch-id";
		String url = "http://" + leaderSelector.getLeader().getId() + ":" + snowflakeConfig.getPort() + path;
		int tries = 0;
		while (true) {
			try {
				JSONObject result =
						HttpClientUtil.execute(new HttpGet(url), new HttpResponseCallbackHandlerJsonHandler<>(JSONObject.class));
				if (result.getInteger("code") == 0) {
					return result.getJSONObject("data").getInteger("workerId");
				} else {
					LOGGER.error("call fetchWorkerId error {}", result.toJSONString());
					throw new IllegalStateException(result.getString("msg"));
				}
			} catch (IOException | HttpServerErrorException e) {
				if (tries++ > 2) {
					throw e;
				} else {
					tries++;
				}
			}
		}
	}


	public void registerWorkerId(int workerId) throws Exception {
		SnowflakeIDGenerator.setWorkerId(workerId);
		int tries = 0;
		while (true) {
			try {
				client.create()
				      .forPath("%s/%s".format(snowflakeConfig.getWorkerIdZkPath(), workerId + ""),
				               (getHostname() + ":" + snowflakeConfig.getPort()).getBytes());
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
			client.getData().forPath(snowflakeConfig.getWorkerIdZkPath());
		} catch (Exception e) {
			client.create().withMode(CreateMode.PERSISTENT).forPath(snowflakeConfig.getWorkerIdZkPath(), NONE);
		}
		List<String> children = client.getChildren().forPath(snowflakeConfig.getWorkerIdZkPath());
		for (String child : children) {
			String peer = new String(client.getData().forPath("%s/%s".format(snowflakeConfig.getWorkerIdZkPath(), child)));
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
		leaderLatch = new CountDownLatch(1);
		LOGGER.info("I'm leader {}", getHostname());
		while (isLeader) {
			if (!snowflakeConfig.isSkipSanityChecks()) {
				sanityCheckPeers();
			}

			leaderLatch.await(30, TimeUnit.SECONDS);
		}

	}

	@Override
	public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
		if (curatorFramework.getConnectionStateErrorPolicy().isErrorState(connectionState)) {
			isLeader = false;
			leaderLatch.countDown();
			throw new CancelLeadershipException();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		client = zkClient.getClient();
		leaderSelector = new LeaderSelector(client, leaderPath, this);
	}

	@Override
	public void close() throws Exception {
		leaderSelector.close();
	}

	public static void main(String[] args) throws UnknownHostException {
		System.out.println(new SnowflakeServer().getHostname());
	}



}

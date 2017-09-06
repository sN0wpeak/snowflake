package io.shuidi.snowflake.server;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import io.shuidi.snowflake.client.SnowflakeClient;
import io.shuidi.snowflake.core.config.SnowflakeConfig;
import io.shuidi.snowflake.core.error.ServiceErrorException;
import io.shuidi.snowflake.core.error.enums.ErrorCode;
import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.core.service.PartnerStoreHolder;
import io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator;
import io.shuidi.snowflake.core.util.RetryRunner;
import io.shuidi.snowflake.core.util.zk.ZkUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.CancelLeadershipException;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator.WORKER_ID_MAX_VALUE;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 16:36
 */

@Service
public class SnowflakeServer implements LeaderSelectorListener, Closeable, Watcher, SmartInitializingSingleton {
	private SnowflakeClient snowflakeClient = new SnowflakeClient();
	private static Logger LOGGER = LoggerFactory.getLogger(SnowflakeServer.class);

	private static final byte[] NONE = new byte[]{};

	public void setClient(CuratorFramework client) {
		this.client = client;
	}

	private CuratorFramework client;

	private LeaderSelector leaderSelector;
	private volatile boolean isLeader = false;
	public static final Set<Integer> ALL_WORKER_IDS = ImmutableSortedSet.copyOf(Stream.iterate(0, a -> ++a).limit(1024).iterator());
	private volatile int workerId = -1;

	public void start() throws Exception {
		client.start();

		leaderSelector = new LeaderSelector(client, SnowflakeConfig.getLeaderPath(), this);
		leaderSelector.autoRequeue();
		LOGGER.info("start SnowflakeServer... ip: {}", getHostname());

		leaderSelector.start();
		while (!hasLeader()) {
			Thread.sleep(1000);
		}

		initWorkerId();

		ReporterHolder.metrics.register(MetricRegistry.name("SnowflakeServer", "workerId"), new Gauge<Integer>() {
			@Override
			public Integer getValue() {
				return workerId;
			}
		});
	}


	private void initWorkerId() throws Exception {
		RetryRunner.create()
		           .addTryExceptions(KeeperException.NodeExistsException.class)
		           .onError(e -> ReporterHolder.incException(e))
		           .thenThrow()
		           .run(() -> {
			           int workerId = genWorkid();
			           registerWorkerId(workerId);
			           return null;
		           });
	}

	private int genWorkid() throws Exception {
		return isLeader() ? allocWorkerId() : snowflakeClient.allocWorkerId(getLeaderUrl());
	}


	public boolean hasLeader() throws Exception {
		return leaderSelector.getLeader().isLeader();
	}

	public void releaseLeader() {
		leaderSelector.interruptLeadership();
	}


	public boolean isLeader() {
		return isLeader;
	}

	private Set<Integer> waitRegWorkerIds = Sets.newConcurrentHashSet();

	public synchronized int allocWorkerId() throws Exception {
		Set<Integer> diff = Sets.difference(ALL_WORKER_IDS, peers().keySet());
		diff = Sets.difference(diff, waitRegWorkerIds);
		Optional<Integer> optional = diff.stream().findFirst();
		if (optional.isPresent()) {
			waitRegWorkerIds.add(optional.get());
			return optional.get();
		}
		throw new ServiceErrorException(ErrorCode.SNOW_NOT_MORE_WORKER_ID);
	}

	public String getLeaderUrl() {
		return RetryRunner.create()
		                  .addTryExceptions(KeeperException.NodeExistsException.class)
		                  .onError(e -> {
			                  LOGGER.error("getLeaderUrl", e);
			                  ReporterHolder.incException(e);
		                  })
		                  .limit(10)
		                  .thenThrow()
		                  .run(() -> {
			                  String leaderId = leaderSelector.getLeader().getId();
			                  Optional<Peer> optional =
					                  peers().values().stream().filter(peer -> peer.getHost().equals(leaderId)).findFirst();
			                  return optional.isPresent() ? optional.get().getHostUrl() : null;
		                  });
	}


	public void registerWorkerId(int workerId) throws Exception {
		if (!(workerId >= 0L && workerId < WORKER_ID_MAX_VALUE)) {
			Exception e = new IllegalArgumentException();
			ReporterHolder.incException(e);
			throw e;
		}

		RetryRunner.create()
		           .addTryExceptions(KeeperException.NodeExistsException.class)
		           .onError(e -> {
			           ReporterHolder.incException(e);
			           LOGGER.error("registerWorkerId", e);
		           })
		           .thenThrow()
		           .run((Callable<Void>) () -> {
			           String path = String.format("%s/%s", SnowflakeConfig.getWorkerIdZkPath(), workerId + "");
			           LOGGER.info("registerWorkerId: {} path:{}", workerId, path);
			           client.create().withMode(CreateMode.EPHEMERAL)
			                 .forPath(path,
			                          (getHostname() + ":" + SnowflakeConfig.getPort()).getBytes());
			           this.workerId = workerId;
			           waitRegWorkerIds.add(workerId);
			           return null;
		           });

	}

	public static String getHostname() throws UnknownHostException {return InetAddress.getLocalHost().getHostAddress();}

	public void sanityCheckPeers() throws Exception {
		final int[] peerCount = {0};
		ImmutableMap<Integer, Peer> peers = peers();
		List<Long> timestamps = peers.entrySet()
		                             .stream()
		                             .filter(peer ->
				                                     !peer.getValue().getHost()
				                                          .equals(SnowflakeConfig.getServer()) &&
				                                     peer.getValue().getPort() != SnowflakeConfig.getPort()
		                             ).map(integerPeerEntry -> {
					try {
						Stopwatch stopwatch = Stopwatch.createStarted();
						int id = integerPeerEntry.getKey();
						Peer peer = integerPeerEntry.getValue();
						LOGGER.info("connecting to {}:{}", peer.getHost(), peer.getPort());

						int reportedWorkerId = snowflakeClient.getWorkerId(integerPeerEntry.getValue().getHostUrl());
						if (reportedWorkerId != id) {
							LOGGER.error("Worker at {}:{} has id {} in zookeeper, but via rpc it says {}", peer.getHost(), peer.port, id,
							             reportedWorkerId);
							ReporterHolder.incException(IllegalStateException.class);
							throw new IllegalStateException("Worker id insanity.");
						}
						peerCount[0]++;
						return stopwatch.stop().elapsed(TimeUnit.MILLISECONDS);
					} catch (Exception e) {
						LOGGER.error("", e);
						ReporterHolder.incException(e);
						throw new IllegalStateException(e);
					}
				}).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(timestamps)) {
			long avg = timestamps.stream().reduce(0L, (a, b) -> a + b) / peerCount[0];
			if (Math.abs(avg) > 10000) {
				LOGGER.error("Timestamp sanity check failed. Mean timestamp is {}, " +
				             "More than 10s away from the mean", avg);
				ReporterHolder.incException(IllegalStateException.class);
				throw new IllegalStateException("timestamp sanity check failed");
			}
		}
	}

	public ImmutableMap<Integer, Peer> peers() throws Exception {
		/**
		 * 如果
		 */
		ImmutableMap.Builder<Integer, Peer> peerBuilder = ImmutableMap.builder();
		try {
			client.getData().forPath(SnowflakeConfig.getWorkerIdZkPath());
		} catch (Exception e) {
			client.create().withMode(CreateMode.PERSISTENT).forPath(SnowflakeConfig.getWorkerIdZkPath(), NONE);
		}
		List<String> children = client.getChildren().forPath(SnowflakeConfig.getWorkerIdZkPath());
		for (String child : children) {
			String chidPath = String.format("%s/%s", SnowflakeConfig.getWorkerIdZkPath(), child);
			LOGGER.info(chidPath);
			String peer = new String(client.getData().forPath(chidPath));
			if (StringUtils.isEmpty(peer)) {
				continue;
			}
			String[] list = peer.split(":");
			peerBuilder.put(Integer.valueOf(child), new Peer(list[0], Integer.valueOf(list[1])));
		}
		ImmutableMap<Integer, Peer> peerImmutableMap = peerBuilder.build();
//		LOGGER.info("found {} children", peerImmutableMap.size());
		return peerImmutableMap;
	}

	@Override
	public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
		isLeader = true;
		LOGGER.info("I'm leader {}", getHostname());
		int tries = 0;
		updateWaitWorkerId();
		while (isLeader()) {
			try {
				if (!SnowflakeConfig.isSkipSanityChecks()) {
					sanityCheckPeers();
					LOGGER.info("waitRegWorkerIds {} ", waitRegWorkerIds);
				}
				Thread.sleep(30000);
				tries = 0;
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					LOGGER.info("Interrupting leader...");
					Thread.currentThread().interrupt();
					break;
				} else {
					if (tries++ > 2) {
						ReporterHolder.incException(e);
						LOGGER.error("", e);
						break;
					} else {
						Thread.sleep(1000);
					}
				}
			}
		}
		isLeader = false;
		client.watches().remove(this);

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
		waitRegWorkerIds.clear();
	}


	@Override
	public void close() {
		CloseableUtils.closeQuietly(leaderSelector);
		CloseableUtils.closeQuietly(client);
	}

	public int getWorkerId() {
		return workerId;
	}

	@Override
	public void process(WatchedEvent watchedEvent) {
		LOGGER.info(String.format("%s/%s/%s", watchedEvent.getPath(), watchedEvent.getState(), watchedEvent.getType()));
		if (isLeader()) {
			if (watchedEvent.getState() == Event.KeeperState.Disconnected) {
				callback();
			} else if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
				callback();
			}
		}
	}

	public void callback() {
		updateWaitWorkerId();
	}


	public void updateWaitWorkerId() {
		int tries = 0;
		while (!Thread.currentThread().isInterrupted()) {
			try {
				client.getChildren().usingWatcher(this).forPath(SnowflakeConfig.getWorkerIdZkPath());
				List<Integer> workerIds = peers().keySet().asList();
				waitRegWorkerIds.removeAll(workerIds);
				break;
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					LOGGER.info("Interrupting leader...");
					Thread.currentThread().interrupt();
				} else {
					if (tries++ > 2) {
						ReporterHolder.incException(e);
						LOGGER.error("", e);
						break;
					}
				}
			}
		}
	}

	@Override
	public void afterSingletonsInstantiated() {

		this.setClient(ZkUtils.create());
		try {
			this.start();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		if (this.getWorkerId() > 0) {
			SnowflakeIDGenerator.setWorkerId(this.getWorkerId());
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				close();
			} catch (Exception e) {
			}
		}));
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

		public String getHostUrl() {
			return this.host + ":" + this.port;
		}
	}

}

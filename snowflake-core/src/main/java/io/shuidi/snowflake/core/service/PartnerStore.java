package io.shuidi.snowflake.core.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.shuidi.snowflake.core.error.ServiceErrorException;
import io.shuidi.snowflake.core.error.enums.ErrorCode;
import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.core.util.RetryRunner;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 14:28
 */
public class PartnerStore implements Watcher {
	private final CuratorFramework client;
	private String partnerStorePath;
	private static Logger LOGGER = LoggerFactory.getLogger(PartnerStore.class);

	public PartnerStore(CuratorFramework client, String partnerStorePath) {
		this.client = client;
		this.partnerStorePath = partnerStorePath;
	}

	private Map<String, Partner> partnerMap = new HashMap<>();


	private final AtomicBoolean intied = new AtomicBoolean(false);

	public void init() {
		if (intied.compareAndSet(false, true)) {
			if (client.getState() != CuratorFrameworkState.STARTED) {
				client.start();
			}
			addListioner(new PartnerChangedListener() {
				@Override
				public void onAdd(List<String> partners) {
					if (CollectionUtils.isEmpty(partners)) {
						return;
					}
					LOGGER.info("Partner add new... start");
					for (String biz : partners) {
						RetryRunner.create().onFinalError(e -> {
							LOGGER.error("onAddPartner.error." + biz, e);
							ReporterHolder.incException(e);
						}).includeExceptions(KeeperException.class).run(() -> {
							String partnerPath = ZKPaths.makePath(partnerStorePath, biz);
							client.sync().forPath(partnerPath);
							Partner partner = JSONObject.parseObject(client.getData().forPath(partnerPath), Partner.class);
							partnerMap.put(biz, partner);
							LOGGER.info("Partner add new..., Partner: {}, bizInfo: {}", biz, partner);
							return null;
						});
					}
					LOGGER.info("Partner add new.. done Partners: {}", partnerMap);
				}

				@Override
				public void onDelete(List<String> partners) {
					if (CollectionUtils.isEmpty(partners)) {
						return;
					}
					LOGGER.info("Partner del... start");
					for (String biz : partners) {
						LOGGER.info("Partner del... Partner {}", biz);
						partnerMap.remove(biz);
					}
					LOGGER.info("Partner del.. done Partners: {}", partnerMap);
				}

				@Override
				public void onBizInfoChange(Map<String, Partner> partnerMap) {

				}
			});
			handlePartner();
		}
	}

	public void addPartner(String key, Partner partner) {
		String partnerPath = ZKPaths.makePath(partnerStorePath, key);
		RetryRunner.create().onFinalError(e -> {
			LOGGER.error("addPartner.error", e);
			ReporterHolder.incException(e);
			throw new ServiceErrorException(ErrorCode.SYSTEM_ERROR);
		}).run((Callable<Void>) () -> {
			if (client.checkExists().creatingParentsIfNeeded().forPath(partnerPath) != null) {
				client.setData()
				      .forPath(partnerPath, JSONObject.toJSONBytes(partner));
			} else {
				client.create()
				      .creatingParentsIfNeeded()
				      .withMode(CreateMode.PERSISTENT)
				      .forPath(partnerPath, JSONObject.toJSONBytes(partner));
			}
			return null;
		});
	}

	public void removePartner(String key) {
		RetryRunner.create()
		           .excludeExceptions(KeeperException.NoNodeException.class)
		           .includeExceptions(KeeperException.class)
		           .onFinalError(e -> {
			           if (e instanceof KeeperException.NoNodeException) {
			           } else {
				           ReporterHolder.incException(e);
				           LOGGER.error("removePartner.error", e);
				           throw new ServiceErrorException(ErrorCode.SYSTEM_ERROR);
			           }
		           })
		           .run((Callable<Void>) () -> {
			           client.delete()
			                 .forPath(ZKPaths.makePath(partnerStorePath, key));
			           return null;
		           });
	}

	public Map<String, Partner> getPartnerMap() {
		return Maps.newHashMap(partnerMap);
	}

	public Partner getPartner(String key) {
		return partnerMap.get(key);
	}

	public void handlePartner() {
		RetryRunner.create()
		           .includeExceptions(KeeperException.class)
		           .onFinalError(e -> {
			           LOGGER.error("PartnerStore.init", e);
			           ReporterHolder.incException(e);
		           })
		           .run((Callable<Void>) () -> {
			           if (client.checkExists().creatingParentsIfNeeded().forPath(partnerStorePath) == null) {
				           client.create()
				                 .creatingParentsIfNeeded()
				                 .withMode(CreateMode.PERSISTENT)
				                 .forPath(partnerStorePath);
			           }
			           Set<String> bizs =
					           Sets.newHashSet(client.getChildren().usingWatcher(PartnerStore.this).forPath(partnerStorePath));
			           onEvent(bizs);
			           return null;
		           });
	}

	private void onEvent(Set<String> bizs) {
		List<String> addBizs = ImmutableList.copyOf(Sets.difference(bizs, partnerMap.keySet()));
		List<String> deleteBizs = ImmutableList.copyOf(Sets.difference(partnerMap.keySet(), bizs));
		LOGGER.info("new bizs {}", addBizs);
		LOGGER.info("will delete bizs {}", deleteBizs);
		for (PartnerChangedListener bizChangedListener : bizChangedListeners) {
			try {
				bizChangedListener.onAdd(addBizs);
				bizChangedListener.onDelete(deleteBizs);
			} catch (Exception e) {
				LOGGER.error(PartnerChangedListener.class.getSimpleName() + ".error!", e);
			}
		}
	}


	@Override
	public void process(WatchedEvent event) {
		LOGGER.info("on PartnerStore changed... event: {}", event);
		handlePartner();
	}

	private List<PartnerChangedListener> bizChangedListeners = new CopyOnWriteArrayList<>();

	public void addListioner(PartnerChangedListener bizChangedListener) {
		bizChangedListeners.add(bizChangedListener);
	}

}

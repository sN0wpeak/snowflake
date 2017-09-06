package io.shuidi.snowflake.core.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.shuidi.snowflake.core.error.ServiceErrorException;
import io.shuidi.snowflake.core.error.enums.ErrorCode;
import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.core.util.RetryRunner;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			client.start();
			addListioner(new PartnerChangedListener() {
				@Override
				public void onAdd(List<String> partners) {
					for (String biz : partners) {
						RetryRunner.create().thenThrow().run(() -> {
							String partnerPath = ZKPaths.makePath(partnerStorePath, biz);
							client.sync().forPath(partnerPath);
							partnerMap.put(biz, JSONObject.parseObject(client.getData().forPath(partnerPath), Partner.class));
							return null;
						});
					}
				}

				@Override
				public void onDelete(List<String> partners) {
					for (String biz : partners) {
						partnerMap.remove(biz);
					}
				}

				@Override
				public void onBizInfoChange(Map<String, Partner> partnerMap) {

				}
			});
			handlePartner();
		}
	}

	public void addPartner(String key, Partner partner) {
		RetryRunner.create().thenThrow().run((Callable<Void>) () -> {
			try {
				client.create()
				      .creatingParentsIfNeeded()
				      .withMode(CreateMode.PERSISTENT)
				      .forPath(ZKPaths.makePath(partnerStorePath, key), JSONObject.toJSONBytes(partner));
			} catch (Exception e) {
				if (e instanceof KeeperException.NodeExistsException) {
					client.setData()
					      .forPath(ZKPaths.makePath(partnerStorePath, key), JSONObject.toJSONBytes(partner));
				} else {
					LOGGER.error("addPartner.error", e);
					throw new ServiceErrorException(ErrorCode.SYSTEM_ERROR);
				}
			}
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
		           .addTryExceptions(KeeperException.NoNodeException.class)
		           .onError(e -> {
			           LOGGER.error("PartnerStore.init", e);
			           ReporterHolder.incException(e);
		           })
		           .run((Callable<Void>) () -> {
			           try {
				           Set<String> bizs =
						           Sets.newHashSet(client.getChildren().usingWatcher(PartnerStore.this).forPath(partnerStorePath));
				           onEvent(bizs);
			           } catch (Exception e) {
				           if (e instanceof KeeperException.NoNodeException) {
					           client.create()
					                 .creatingParentsIfNeeded()
					                 .withMode(CreateMode.PERSISTENT)
					                 .forPath(partnerStorePath);
				           }
			           }
			           return null;
		           });
	}

	private void onEvent(Set<String> bizs) {
		Set<String> addBizs = Sets.difference(bizs, partnerMap.keySet());
		Set<String> deleteBizs = Sets.difference(partnerMap.keySet(), bizs);
		for (PartnerChangedListener bizChangedListener : bizChangedListeners) {
			try {
				bizChangedListener.onAdd(Lists.newArrayList(addBizs));
				bizChangedListener.onDelete(Lists.newArrayList(deleteBizs));
			} catch (Exception e) {
				LOGGER.error(PartnerChangedListener.class.getSimpleName() + ".error!", e);
			}
		}
	}


	@Override
	public void process(WatchedEvent event) {
		handlePartner();
	}

	private List<PartnerChangedListener> bizChangedListeners = new CopyOnWriteArrayList<>();

	public void addListioner(PartnerChangedListener bizChangedListener) {
		bizChangedListeners.add(bizChangedListener);
	}

}

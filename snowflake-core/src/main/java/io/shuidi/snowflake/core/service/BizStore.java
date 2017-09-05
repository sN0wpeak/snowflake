package io.shuidi.snowflake.core.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
public class BizStore implements Watcher {
	private final CuratorFramework client;
	private String bizStorePath;
	private static Logger LOGGER = LoggerFactory.getLogger(BizStore.class);

	public BizStore(CuratorFramework client, String bizStorePath) {
		this.client = client;
		this.bizStorePath = bizStorePath;
	}

	private Map<String, BizInfo> bizsMap = new HashMap<>();


	private final AtomicBoolean intied = new AtomicBoolean(false);

	public void init() {
		if (intied.compareAndSet(false, true)) {
			addListioner(new BizChangedListener() {
				@Override
				public void onAdd(List<String> bizs) {
					for (String biz : bizs) {
						try {
							String bizPath = ZKPaths.makePath(bizStorePath, biz);
							client.sync().forPath(bizPath);
							bizsMap.put(biz, JSONObject.parseObject(client.getData().forPath(bizPath), BizInfo.class));
						} catch (Exception e) {
							LOGGER.error("onAdd", e);
						}
					}
				}

				@Override
				public void onDelete(List<String> bizs) {
					for (String biz : bizs) {
						bizsMap.remove(biz);
					}
				}

				@Override
				public void onBizInfoChange(Map<String, BizInfo> infoMap) {

				}
			});
			handleBizs();
		}
	}

	public void addBiz(String key, BizInfo bizInfo) {
		RetryRunner.create().thenThrow().run(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				client.create()
				      .creatingParentsIfNeeded()
				      .withMode(CreateMode.PERSISTENT)
				      .forPath(ZKPaths.makePath(bizStorePath, key), JSONObject.toJSONBytes(bizInfo));
				return null;
			}
		});
	}

	public Map<String, BizInfo> getBizsMap() {
		return Maps.newHashMap(bizsMap);
	}

	public BizInfo getBiz(String key) {
		return bizsMap.get(key);
	}

	public void handleBizs() {
		RetryRunner.create()
		           .addTryExceptions(KeeperException.NoNodeException.class)
		           .onError(e -> {
			           LOGGER.error("BizStore.init", e);
			           ReporterHolder.incException(e);
		           })
		           .run((Callable<Void>) () -> {
			           try {
				           Set<String> bizs = Sets.newHashSet(client.getChildren().usingWatcher(BizStore.this).forPath(bizStorePath));
				           onEvent(bizs);
			           } catch (Exception e) {
				           if (e instanceof KeeperException.NoNodeException) {
					           client.create()
					                 .creatingParentsIfNeeded()
					                 .withMode(CreateMode.PERSISTENT)
					                 .forPath(bizStorePath);
				           }
			           }
			           return null;
		           });
	}

	private void onEvent(Set<String> bizs) {
		Set<String> addBizs = Sets.difference(bizsMap.keySet(), bizs);
		Set<String> deleteBizs = Sets.difference(bizs, bizsMap.keySet());
		for (BizChangedListener bizChangedListener : bizChangedListeners) {
			try {
				bizChangedListener.onAdd(Lists.newArrayList(addBizs));
				bizChangedListener.onDelete(Lists.newArrayList(deleteBizs));
			} catch (Exception e) {
				LOGGER.error("BizChangedListener.error " + BizChangedListener.class, e);
			}
		}
	}


	@Override
	public void process(WatchedEvent event) {
		handleBizs();
	}

	private List<BizChangedListener> bizChangedListeners = new CopyOnWriteArrayList<>();

	public void addListioner(BizChangedListener bizChangedListener) {
		bizChangedListeners.add(bizChangedListener);
	}

}

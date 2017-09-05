package io.shuidi.snowflake.core.service;

import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 14:56
 */
public class BizStoreHolder {

	private static BizStore bizStore;
	private static final AtomicBoolean intied = new AtomicBoolean(false);

	public static void init(CuratorFramework client, String bizStorePath) {
		if (intied.compareAndSet(false, true)) {
			bizStore = new BizStore(client, bizStorePath);
			bizStore.init();
		}
	}


	public static BizStore getBizStore() {
		return bizStore;
	}
}

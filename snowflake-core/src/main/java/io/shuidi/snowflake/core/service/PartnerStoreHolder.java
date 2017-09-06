package io.shuidi.snowflake.core.service;

import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 14:56
 */
public class PartnerStoreHolder {

	private static volatile PartnerStore bizStore;
	private static final AtomicBoolean intied = new AtomicBoolean(false);

	public static void init(CuratorFramework client, String bizStorePath) {
		if (intied.compareAndSet(false, true)) {
			bizStore = new PartnerStore(client, bizStorePath);
			bizStore.init();
		}
	}


	public static PartnerStore getBizStore() {
		return bizStore;
	}
}

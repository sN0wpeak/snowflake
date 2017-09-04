package io.shuidi.snowflake.core.util.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 19:41
 */
public class CuratorFrameworkUtils {

	public static CuratorFramework create(String connectionString, int connectionTimeoutMs, int sessionTimeoutMs) {
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
		return CuratorFrameworkFactory.builder()
		                              .connectString(connectionString)
		                              .retryPolicy(retryPolicy)
		                              .connectionTimeoutMs(connectionTimeoutMs)
		                              .sessionTimeoutMs(sessionTimeoutMs)
		                              .build();
	}

}

package io.shuidi.snowflake.core.util.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 19:41
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ZkUtils {


	private static String connectionString;
	private static int connectionTimeoutMs = 3000;
	private static int sessionTimeoutMs = 10000;

	public static CuratorFramework create(String connectionString, int connectionTimeoutMs, int sessionTimeoutMs) {
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
		return CuratorFrameworkFactory.builder()
		                              .connectString(connectionString)
		                              .retryPolicy(retryPolicy)
		                              .connectionTimeoutMs(connectionTimeoutMs)
		                              .sessionTimeoutMs(sessionTimeoutMs)
		                              .build();
	}

	public static CuratorFramework create() {
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
		return CuratorFrameworkFactory.builder()
		                              .connectString(connectionString)
		                              .retryPolicy(retryPolicy)
		                              .connectionTimeoutMs(connectionTimeoutMs)
		                              .sessionTimeoutMs(sessionTimeoutMs)
		                              .build();
	}

	public static String getConnectionString() {
		return connectionString;
	}

	@Value("${snowflake.zk.hosts}")
	public void setConnectionString(String connectionString) {
		ZkUtils.connectionString = connectionString;
	}

	public static int getConnectionTimeoutMs() {
		return connectionTimeoutMs;
	}

	@Value("${snowflake.zk.connectionTimeout}")
	public void setConnectionTimeoutMs(int connectionTimeoutMs) {
		ZkUtils.connectionTimeoutMs = connectionTimeoutMs;
	}

	public static int getSessionTimeoutMs() {
		return sessionTimeoutMs;
	}

	@Value("${snowflake.zk.sessionTimeout}")
	public void setSessionTimeoutMs(int sessionTimeoutMs) {
		ZkUtils.sessionTimeoutMs = sessionTimeoutMs;
	}

}

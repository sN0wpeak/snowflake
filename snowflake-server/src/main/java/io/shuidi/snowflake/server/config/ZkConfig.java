package io.shuidi.snowflake.server.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 21:15
 */
@Configuration
public class ZkConfig {
	@Value("${snowflake.zk.hosts}")
	private String connectionString;
	@Value("${snowflake.zk.connectionTimeout}")
	private int connectionTimeoutMs = 3000;
	@Value("${snowflake.zk.sessionTimeout}")
	private int sessionTimeoutMs = 10000;

	public String getConnectionString() {
		return connectionString;
	}

	public int getConnectionTimeoutMs() {
		return connectionTimeoutMs;
	}

	public int getSessionTimeoutMs() {
		return sessionTimeoutMs;
	}

	@Bean
	public CuratorFramework zkClient() {
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
		return CuratorFrameworkFactory.builder()
		                              .connectString(getConnectionString())
		                              .retryPolicy(retryPolicy)
		                              .connectionTimeoutMs(getConnectionTimeoutMs())
		                              .sessionTimeoutMs(getSessionTimeoutMs())
		                              .build();
	}

	public void setConnectionString(String connectionString) {
		this.connectionString = connectionString;
	}

	public void setConnectionTimeoutMs(int connectionTimeoutMs) {
		this.connectionTimeoutMs = connectionTimeoutMs;
	}

	public void setSessionTimeoutMs(int sessionTimeoutMs) {
		this.sessionTimeoutMs = sessionTimeoutMs;
	}
}

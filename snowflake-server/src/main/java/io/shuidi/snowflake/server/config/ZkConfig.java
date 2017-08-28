package io.shuidi.snowflake.server.config;

import org.springframework.context.annotation.Configuration;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 21:15
 */
@Configuration
public class ZkConfig {
	private String connectionString;
	private int connectionTimeoutMs = 3000;
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
}

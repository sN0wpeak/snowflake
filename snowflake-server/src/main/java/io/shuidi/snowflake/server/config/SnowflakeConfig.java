package io.shuidi.snowflake.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 17:21
 */
@Configuration
public class SnowflakeConfig {

	@Value("${snowflake.config.workerIdZkPath}")
	private String workerIdZkPath;
	private boolean skipSanityChecks = false;
	private long startupSleepMs = 10000;
	private int datacenterId = 0;
	@Value("${server.port}")
	private int port;
	private String server;

	public String getServer() {
		return server;
	}

	public int getPort() {
		return port;
	}

	public SnowflakeConfig() {

	}

	public String getWorkerIdZkPath() {
		return workerIdZkPath;
	}


	public boolean isSkipSanityChecks() {
		return skipSanityChecks;
	}

	public long getStartupSleepMs() {
		return startupSleepMs;
	}

	public int getDatacenterId() {
		return datacenterId;
	}
}

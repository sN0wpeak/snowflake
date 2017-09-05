package io.shuidi.snowflake.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 17:21
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SnowflakeConfig {


	private static String workerIdZkPath;
	private static String leaderPath;
	private static boolean skipSanityChecks = false;
	private static long startupSleepMs = 10000;
	private static int datacenterId = 0;
	private static String lockPath;
	private static String storePath;
	private static int port;
	private static String server;

	public SnowflakeConfig() {

	}

	public static String getLeaderPath() {
		return leaderPath;
	}

	@Value("${snowflake.config.leaderPath}")
	public void setLeaderPath(String leaderPath) {
		SnowflakeConfig.leaderPath = leaderPath;
	}

	public static String getLockPath() {
		return lockPath;
	}

	@Value("${snowflake.config.lockPath}")
	public void setLockPath(String lockPath) {
		SnowflakeConfig.lockPath = lockPath;
	}

	public static String getStorePath() {
		return storePath;
	}

	@Value("${snowflake.config.storePath}")
	public void setStorePath(String storePath) {
		SnowflakeConfig.storePath = storePath;
	}

	public static String getServer() {
		return server;
	}

	public void setServer(String server) {
		SnowflakeConfig.server = server;
	}

	public static int getPort() {
		return port;
	}

	@Value("${server.port}")
	public void setPort(int port) {
		SnowflakeConfig.port = port;
	}

	public static String getWorkerIdZkPath() {
		return workerIdZkPath;
	}

	@Value("${snowflake.config.workerIdZkPath}")
	public void setWorkerIdZkPath(String workerIdZkPath) {
		SnowflakeConfig.workerIdZkPath = workerIdZkPath;
	}

	public static boolean isSkipSanityChecks() {
		return skipSanityChecks;
	}

	public void setSkipSanityChecks(boolean skipSanityChecks) {
		SnowflakeConfig.skipSanityChecks = skipSanityChecks;
	}

	public static long getStartupSleepMs() {
		return startupSleepMs;
	}

	public void setStartupSleepMs(long startupSleepMs) {
		SnowflakeConfig.startupSleepMs = startupSleepMs;
	}

	public static int getDatacenterId() {
		return datacenterId;
	}

	public void setDatacenterId(int datacenterId) {
		SnowflakeConfig.datacenterId = datacenterId;
	}

}

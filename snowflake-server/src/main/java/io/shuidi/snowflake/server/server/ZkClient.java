package io.shuidi.snowflake.server.server;

import io.shuidi.snowflake.server.config.ZkConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 21:15
 */
@Service
public class ZkClient implements InitializingBean {

	@Autowired
	ZkConfig zkConfig;
	private CuratorFramework client;

	public CuratorFramework getClient() {
		return client;
	}

	private CuratorFramework createClient() {
		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
		return CuratorFrameworkFactory.builder()
		                              .connectString(zkConfig.getConnectionString())
		                              .retryPolicy(retryPolicy)
		                              .connectionTimeoutMs(zkConfig.getConnectionTimeoutMs())
		                              .sessionTimeoutMs(zkConfig.getSessionTimeoutMs())
		                              .build();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		client = createClient();
	}
}

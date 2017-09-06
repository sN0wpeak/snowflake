package io.shuidi.snowflake.core.util.sequence;

import io.shuidi.snowflake.core.util.zk.ZkUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 22:56
 */
public class TestSeqBase {
	protected List<String> clients =
			org.assertj.core.util.Lists.newArrayList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N");

	public ZkRangeStore getZkRangeStore(int clientIndex) {
		String lockPath = "/snowflake/locks";
		String storePath = "/snowflake/idstore";
		CuratorFramework curatorFramework = ZkUtils.create("127.0.0.1:2181", 1000, 10000);
		curatorFramework.start();
		for (String client : clients) {
			try {
				curatorFramework.setData().forPath(ZKPaths.makePath(storePath, client), "0".getBytes());
			} catch (Exception e) {
				if (e instanceof KeeperException.NoNodeException) {
					try {
						curatorFramework.create().creatingParentsIfNeeded().forPath(storePath, "0".getBytes());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		return new ZkRangeStore(clients.get(clientIndex), curatorFramework, lockPath, storePath, 1, TimeUnit.SECONDS, 0, 10);
	}
}

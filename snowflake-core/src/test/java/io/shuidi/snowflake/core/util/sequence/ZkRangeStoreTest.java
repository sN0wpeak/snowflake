package io.shuidi.snowflake.core.util.sequence;

import io.shuidi.snowflake.core.util.zk.CuratorFrameworkUtils;
import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 19:37
 */
public class ZkRangeStoreTest {

	@Test
	public void getNextRange() throws Exception {
		String lockPath = "/snowflake/locks";
		String storePath = "/snowflake/idstore";
		List<String> clients = Lists.newArrayList("A", "B", "C");
		CuratorFramework curatorFramework = CuratorFrameworkUtils.create("127.0.0.1:2181", 1000, 10000);
		curatorFramework.start();
		for (int i = 0; i < 100; i++) {
			ZkRangeStore zkRangeStore =
					new ZkRangeStore(clients.get(i % 3), curatorFramework, lockPath, storePath, 1, TimeUnit.SECONDS, 0, 10);
			new Thread(() -> {
				for (int j = 0; j < 100; j++) {
					System.out.println(zkRangeStore.getNextRange());
				}
			}).start();
		}

	}

}
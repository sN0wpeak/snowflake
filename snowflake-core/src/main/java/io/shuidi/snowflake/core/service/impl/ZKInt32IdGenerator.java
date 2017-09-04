package io.shuidi.snowflake.core.service.impl;

import io.shuidi.snowflake.core.service.IDGenerator;
import io.shuidi.snowflake.core.util.sequence.RangeSequence;
import io.shuidi.snowflake.core.util.sequence.RangeStore;
import io.shuidi.snowflake.core.util.sequence.ZkRangeStore;
import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.TimeUnit;

/**
 * Author: Alvin Tian
 * Date: 2017/9/3 10:59
 */
public class ZKInt32IdGenerator implements IDGenerator<Integer> {

	private final CuratorFramework client;

	private RangeSequence rangeSequence;

	public ZKInt32IdGenerator(CuratorFramework client, String lockPath, String storePath, String name, long start, int rangeCount) {
		this.client = client;
		RangeStore rangeStore = new ZkRangeStore(name, client, lockPath, storePath, 1, TimeUnit.SECONDS, start, rangeCount);
		if (start < 0) {
			start = rangeStore.getNextRange();
		}
		rangeSequence = new RangeSequence(1, start, rangeCount, rangeStore);
		rangeSequence.start();
	}

	@Override
	public Integer generateId() {
		return (int) rangeSequence.incrementAndGet();
	}

}

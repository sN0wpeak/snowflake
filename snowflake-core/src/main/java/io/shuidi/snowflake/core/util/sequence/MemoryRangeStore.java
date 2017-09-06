package io.shuidi.snowflake.core.util.sequence;

import io.shuidi.snowflake.core.report.ReporterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 11:22
 */
public class MemoryRangeStore implements RangeStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(MemoryRangeStore.class);
	private Sequence sequence;

	public MemoryRangeStore(long start, long increment) {
		this.sequence = new Sequence(start, increment);
	}

	@Override
	public long getNextRange() {
		long v = sequence.getAndIncrement();
		LOGGER.info(Thread.currentThread().getName() + " getNextRange {}", v);
		return v;
	}

	@Override
	public long getCurrRange() {
		return sequence.get();
	}

	@Override
	public void close() throws IOException {

	}
}

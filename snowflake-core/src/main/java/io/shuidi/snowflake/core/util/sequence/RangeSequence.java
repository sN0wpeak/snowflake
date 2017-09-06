package io.shuidi.snowflake.core.util.sequence;

import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import io.shuidi.snowflake.core.error.ServiceErrorException;
import io.shuidi.snowflake.core.error.enums.ErrorCode;
import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.core.util.UnsafeUtils;
import io.shuidi.snowflake.core.util.threadpool.ThreadPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.io.IOException;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Author: Alvin Tian
 * Date: 2017/9/3 11:15
 */
public class RangeSequence {
	private static final Logger LOGGER = LoggerFactory.getLogger(RangeSequence.class);

	private final Sequence sequence;
	private final long increment;
	private final long start;
	private final long rangeCount;
	private final RangeStore rangeStore;
	private static final long SIZECTL;

	private transient volatile long sizeCtl;
	private transient volatile long valueWhenInterrupted = -1;

	private Exchanger<Long> exchanger = new Exchanger<>();
	private final AtomicInteger ctl = new AtomicInteger(INITIAL);
	private static final int INITIAL = 1;
	private static final int RUNNING = 1 << INITIAL;
	private static final int STOP = 1 << RUNNING;

	public RangeSequence(long increment, long start, long rangeCount, RangeStore rangeStore) {
		Preconditions.checkNotNull(rangeStore);
		Preconditions.checkState(increment < rangeCount);
		Preconditions.checkState(start + rangeCount < Long.MAX_VALUE);
		this.increment = increment;
		this.rangeCount = rangeCount;
		this.rangeStore = rangeStore;
		this.start = start;
		this.sequence = new Sequence(start, increment);
		this.sizeCtl = start + rangeCount;
	}

	//	static final int resizeStamp(int n) {
//		return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
//	}

	public long incrementAndGet() {
//		long v = sequence.get();
//		long sc;
//		while ((v >= (sc = sizeCtl)) && v <= Long.MAX_VALUE) {
//			if (sc < -1) {
//				Thread.yield();
//			} else if (U.compareAndSwapLong(this, SIZECTL, sc, -1)) {
//				try {
//					v = exchanger.exchange(v);
//				} catch (InterruptedException e) {
//					int tries = 0;
//					while (valueWhenInterrupted < 0) {
//						if (++tries % 1000 == 0) {
//							try {
//								Thread.sleep(100);
//							} catch (InterruptedException ig) {
//							}
//						}
//					}
//					LOGGER.info("valueWhenInterrupted {}", valueWhenInterrupted);
//					v = valueWhenInterrupted;
//				}
//				sequence.set(v);
//				sizeCtl = v + rangeCount;
//			}
//		}
//		v = sequence.incrementAndGet();
//		return v;

		if (ctl.get() == STOP) {
			throw new ServiceErrorException(ErrorCode.SYSTEM_STOP);
		}

		// TODO: 2017/9/4 待优化

		synchronized (this) {
			long v = sequence.incrementAndGet();
			long sc;

			if ((v > (sc = sizeCtl)) && v <= Long.MAX_VALUE) {
				v = getNextValue(v);
				sizeCtl = v + rangeCount;
				v = v + 1;
				sequence.set(v);
			}

			return v;
		}

	}

	public long getNextValue(long v) {

		int tries = 0;
		while (true) {
			try {
				v = exchanger.exchange(v, 100, TimeUnit.MILLISECONDS);
				return v;
			} catch (InterruptedException e) {
				if (ctl.get() == STOP) {
					throw new ServiceErrorException(ErrorCode.SYSTEM_STOP);
				} else {
					throw new ServiceErrorException(ErrorCode.SYSTEM_ERROR);
				}
			} catch (TimeoutException e) {
				if (tries++ > 2) {
					throw new ServiceErrorException(ErrorCode.SNOW_GET_NEXT_SEQUENCE_ERROR);
				}
			}
		}
	}

	private final static Unsafe U = UnsafeUtils.getUnsafe();

	static {
		Class<?> k = RangeSequence.class;
		try {
			SIZECTL = U.objectFieldOffset
					(k.getDeclaredField("sizeCtl"));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void stop() {
		if (ctl.compareAndSet(RUNNING, STOP)) {
			LOGGER.info("RangeSequence will stop...");
			try {
				rangeStore.close();
			} catch (IOException e) {
			}
			rangeStoreThread.interrupt();
		}
	}

	private volatile Thread rangeStoreThread;

	public void start() {
		rangeStoreThread = Thread.currentThread();
		if (ctl.compareAndSet(INITIAL, RUNNING)) {
			ReporterHolder.metrics.counter("RangeSeq." + rangeStore.getClass().getSimpleName()).inc();
			ThreadPools.sequenceExecutor.execute(() -> {
				long v = 0;
				while (ctl.get() != STOP || !Thread.currentThread().isInterrupted()) {
					try {

						v = rangeStore.getNextRange();
						exchanger.exchange(v);
					} catch (InterruptedException e) {
						LOGGER.info("valueWhenInterrupted {}", valueWhenInterrupted);
						Thread.currentThread().interrupt();
					}
				}
				saveLeftSeq(v);
				LOGGER.info("RangeSequence.{} stop...", rangeStore.toString());
			});
			ReporterHolder.metrics.counter("RangeSeq." + rangeStore.getClass().getSimpleName()).dec();
			LOGGER.info("RangeSequence.{} start...", rangeStore.toString());
		}
	}

	public void saveLeftSeq(long v) {
		// TODO: 2017/9/6 保存现场
		long curr = sequence.get();
		if (v >= curr) {
		}
	}

	public long get() {
		return sequence.get();
	}
}

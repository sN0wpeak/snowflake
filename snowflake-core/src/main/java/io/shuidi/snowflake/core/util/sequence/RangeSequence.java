package io.shuidi.snowflake.core.util.sequence;

import com.google.common.base.Preconditions;
import io.shuidi.snowflake.core.util.UnsafeUtils;
import io.shuidi.snowflake.core.util.threadpool.ThreadPools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
		// TODO: 2017/9/4 待优化
		synchronized (this) {
			long v = sequence.incrementAndGet();
			long sc;
			if ((v > (sc = sizeCtl)) && v <= Long.MAX_VALUE) {
				try {
					v = exchanger.exchange(v, 100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException | TimeoutException e) {
					int tries = 0;
					while (valueWhenInterrupted < 0) {
						if (++tries % 1000 == 0) {
							try {
								Thread.sleep(100);
							} catch (InterruptedException ig) {
							}
						}
					}
					LOGGER.info("valueWhenInterrupted {}", valueWhenInterrupted);
					v = valueWhenInterrupted;
				}
				sizeCtl = v + rangeCount;
				v = v + 1;
				sequence.set(v);
			}
			return v;
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
		currThread.interrupt();
		currThread = null;
	}

	private Thread currThread;

	public void start() {
		ThreadPools.sequenceExecutor.execute(() -> {
			currThread = Thread.currentThread();
			long v;
			while (!Thread.currentThread().isInterrupted()) {
				v = rangeStore.getNextRange();
				if (v < 0) {
					v = 0;
				}
				try {
					exchanger.exchange(v);
				} catch (InterruptedException e) {
					valueWhenInterrupted = v;
					LOGGER.info("valueWhenInterrupted {}", valueWhenInterrupted);
					Thread.currentThread().interrupt();
				}
			}
		});
	}
}

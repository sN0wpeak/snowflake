package io.shuidi.snowflake.core.util.sequence;

import java.util.concurrent.atomic.AtomicLong;


/**
 * Author: Alvin Tian
 * Date: 2017/9/3 11:44
 */
public class Sequence {

	private final long increment;
//	private static final int INITIAL_START = 0;
//
//	public long get() {
//		return this.valueRef;
//	}
//
//	public Sequence(long initialValue, long increment) {
//		this.increment = increment;
//		valueRef = initialValue;
//	}
//
//	public Sequence() {
//		this(INITIAL_START, 1L);
//	}
//
//	private static final Unsafe UNSAFE = UnsafeUtils.getUnsafe();
//	private static final long VALUE_OFFSET;
//
//
//	public final void set(long valueRef) {
//		this.valueRef = valueRef;x
//	}
//
//	public final long incrementAndGet() {
//		return this.addAndGet(increment);
//	}
//
//	public final long addAndGet(long increment) {
//		return UNSAFE.getAndAddLong(this, VALUE_OFFSET, increment) + increment;
//	}
//
//	public final long getAndAdd(long delta) {
//		return UNSAFE.getAndAddLong(this, VALUE_OFFSET, delta);
//	}
//
//	public final long getAndIncrement() {
//		return UNSAFE.getAndAddLong(this, VALUE_OFFSET, increment);
//	}
//
//
//	public boolean compareAndSet(long currentValue, long newValue) {
//		return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, currentValue, newValue);
//	}
//
//	static {
//		try {
//			VALUE_OFFSET = UNSAFE.objectFieldOffset(Value.class.getDeclaredField("valueRef"));
//		} catch (Exception e) {
//			throw new IllegalStateException(e);
//		}
//	}


	public long get() {
		return this.valueRef.get();
	}

	private final AtomicLong valueRef;

	public Sequence(long initialValue, long increment) {
		this.increment = increment;
		valueRef = new AtomicLong(initialValue);

	}

	public final void set(long newValue) {
		this.valueRef.set(newValue);
	}

	public final long incrementAndGet() {
		return this.addAndGet(increment);
	}

	public final long addAndGet(long increment) {
		return this.valueRef.addAndGet(increment);
	}

	public final long getAndAdd(long delta) {
		return this.valueRef.getAndAdd(delta);
	}

	public final long getAndIncrement() {
		return this.getAndAdd(increment);
	}


}

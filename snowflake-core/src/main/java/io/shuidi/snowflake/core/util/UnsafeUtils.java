package io.shuidi.snowflake.core.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Author: Alvin Tian
 * Date: 2017/9/3 11:34
 */
public final class UnsafeUtils {

	private static final Unsafe THE_UNSAFE;

	public static final Unsafe getUnsafe() {
		return THE_UNSAFE;
	}

	static {
		Field field = null;
		try {
			field = Unsafe.class.getDeclaredField("theUnsafe");
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
		field.setAccessible(true);
		try {
			THE_UNSAFE = (Unsafe) field.get(null);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void main(String[] args) {
		int n = 32;
		System.out.println((n << 1) - (n >>> 1));
	}
}

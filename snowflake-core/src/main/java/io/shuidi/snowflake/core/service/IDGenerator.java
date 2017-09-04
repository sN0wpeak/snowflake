package io.shuidi.snowflake.core.service;

/**
 * Author: Alvin Tian
 * Date: 2017/8/22 19:00
 */
public interface IDGenerator<T extends Number> {
	T generateId();
}

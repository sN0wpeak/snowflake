package io.shuidi.snowflake.core.util.sequence;

import java.io.Closeable;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 11:22
 */
public interface RangeStore extends Closeable {

	public long getNextRange() throws InterruptedException;
	public long getCurrRange();

}

package io.shuidi.snowflake.core.util.sequence;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 11:22
 */
public interface RangeStore {

	public long getNextRange();
	public long getCurrRange();

}

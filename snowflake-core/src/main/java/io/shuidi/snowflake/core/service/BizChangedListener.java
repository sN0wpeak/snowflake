package io.shuidi.snowflake.core.service;

import java.util.List;
import java.util.Map;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 14:42
 */
public interface BizChangedListener {

	public void onAdd(List<String> bizs);

	public void onDelete(List<String> bizs);

	public void onBizInfoChange(Map<String, BizInfo> infoMap);
}

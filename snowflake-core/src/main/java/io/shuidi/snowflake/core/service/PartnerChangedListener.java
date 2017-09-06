package io.shuidi.snowflake.core.service;

import java.util.List;
import java.util.Map;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 14:42
 */
public interface PartnerChangedListener {

	public void onAdd(List<String> partners);

	public void onDelete(List<String> partners);

	public void onBizInfoChange(Map<String, Partner> partnerMap);
}

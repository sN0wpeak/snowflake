package io.shuidi.snowflake.core.report;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.AtomicLongMap;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Author: Alvin Tian
 * Date: 2017/8/30 11:36
 */
@Service
public class Reporter {

	private final AtomicLongMap<String> statMap = AtomicLongMap.create();

	public String report() {
		return JSONObject.toJSONString(statMap);
	}

	public long incr(String key) {
		return statMap.incrementAndGet(key);
	}

	public long update(String key, long v) {
		return statMap.updateAndGet(key, operand -> v);
	}

	public Map<String, Long> asMap() {
		return statMap.asMap();
	}
}

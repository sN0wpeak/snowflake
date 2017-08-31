package io.shuidi.snowflake.server.service;

import io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator;
import io.shuidi.snowflake.server.SnowflakeServer;
import io.shuidi.snowflake.server.enums.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: Alvin Tian
 * Date: 2017/8/28 20:15
 */
@Service
public class SnowflakeService {

	SnowflakeIDGenerator snowflakeIDGenerator = new SnowflakeIDGenerator();

	@Autowired
	SnowflakeServer snowflakeServer;


	public long generateId() {
		return snowflakeIDGenerator.generateId();
	}


	public int getWorkerId() {
		return snowflakeServer.getWorkerId();
	}

	public int allocWorkerId() throws Exception {
		if (snowflakeServer.isLeader()) {
			return snowflakeServer.allocWorkerId();
		} else {
			return ErrorCode.NOT_LEADER.getCode();
		}
	}
}

package io.shuidi.snowflake.server.service;

import io.shuidi.snowflake.core.config.SnowflakeConfig;
import io.shuidi.snowflake.core.error.ServiceErrorException;
import io.shuidi.snowflake.core.error.enums.ErrorCode;
import io.shuidi.snowflake.core.service.MultPartnerInt32IdGenerator;
import io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator;
import io.shuidi.snowflake.server.SnowflakeServer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: Alvin Tian
 * Date: 2017/8/28 20:15
 */
@Service
public class SnowflakeService implements SmartInitializingSingleton{

	SnowflakeIDGenerator snowflakeIDGenerator;
	MultPartnerInt32IdGenerator multBizInt32IdGenerator;

	@Autowired
	SnowflakeServer snowflakeServer;

	public long generateId(String partnerKey) {
		return snowflakeIDGenerator.generateId();
	}

	public int generateId32(String partnerKey) {
		return multBizInt32IdGenerator.generateId(partnerKey);
	}


	public int getWorkerId() {
		return snowflakeServer.getWorkerId();
	}

	public int allocWorkerId() throws Exception {
		if (snowflakeServer.isLeader()) {
			return snowflakeServer.allocWorkerId();
		}
		throw new ServiceErrorException(ErrorCode.SNOW_NOT_A_LEADER);
	}

	public void setMultBizInt32IdGenerator(MultPartnerInt32IdGenerator multBizInt32IdGenerator) {
		this.multBizInt32IdGenerator = multBizInt32IdGenerator;
	}

	public void setSnowflakeIDGenerator(SnowflakeIDGenerator snowflakeIDGenerator) {
		this.snowflakeIDGenerator = snowflakeIDGenerator;
	}

	@Override
	public void afterSingletonsInstantiated() {
		MultPartnerInt32IdGenerator multBizInt32IdGenerator =
				new MultPartnerInt32IdGenerator(SnowflakeConfig.getLockPath(), SnowflakeConfig.getSequencePath());
		SnowflakeIDGenerator snowflakeIDGenerator = new SnowflakeIDGenerator();
		setMultBizInt32IdGenerator(multBizInt32IdGenerator);
		setSnowflakeIDGenerator(snowflakeIDGenerator);
	}
}

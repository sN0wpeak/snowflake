package io.shuidi.snowflake;

import io.shuidi.snowflake.core.config.SnowflakeConfig;
import io.shuidi.snowflake.core.service.PartnerStoreHolder;
import io.shuidi.snowflake.core.util.zk.ZkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 16:37
 */

@SpringBootApplication
public class Application implements SmartInitializingSingleton {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);


	public static void main(String[] args) throws Exception {
		final ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);
	}

	@Override
	public void afterSingletonsInstantiated() {
		PartnerStoreHolder.init(ZkUtils.create(), SnowflakeConfig.getStorePath());
	}
}

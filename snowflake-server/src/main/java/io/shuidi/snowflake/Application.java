package io.shuidi.snowflake;

import io.shuidi.snowflake.core.config.SnowflakeConfig;
import io.shuidi.snowflake.core.service.BizStoreHolder;
import io.shuidi.snowflake.core.service.MultBizInt32IdGenerator;
import io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator;
import io.shuidi.snowflake.core.util.zk.CuratorFrameworkUtils;
import io.shuidi.snowflake.server.SnowflakeServer;
import io.shuidi.snowflake.server.service.SnowflakeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 16:37
 */

@SpringBootApplication
public class Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);


	public static void main(String[] args) throws Exception {
		final ConfigurableApplicationContext applicationContext = SpringApplication.run(Application.class, args);

		BizStoreHolder.init(CuratorFrameworkUtils.create(), SnowflakeConfig.getStorePath());

		SnowflakeServer snowflakeServer = applicationContext.getBean(SnowflakeServer.class);
		snowflakeServer.setZkClient(CuratorFrameworkUtils.create());
		snowflakeServer.start();

		MultBizInt32IdGenerator multBizInt32IdGenerator =
				new MultBizInt32IdGenerator(SnowflakeConfig.getLockPath(), SnowflakeConfig.getStorePath());
		SnowflakeIDGenerator snowflakeIDGenerator = new SnowflakeIDGenerator();
		applicationContext.getBean(SnowflakeService.class).setMultBizInt32IdGenerator(multBizInt32IdGenerator);
		applicationContext.getBean(SnowflakeService.class).setSnowflakeIDGenerator(snowflakeIDGenerator);
		if (snowflakeServer.getWorkerId() > 0) {
			SnowflakeIDGenerator.setWorkerId(snowflakeServer.getWorkerId());
		}
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				snowflakeServer.close();
			} catch (Exception e) {
			}
		}));

	}
}

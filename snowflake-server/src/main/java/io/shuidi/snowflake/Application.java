package io.shuidi.snowflake;

import io.shuidi.snowflake.server.SnowflakeServer;
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
		SnowflakeServer snowflakeServer = applicationContext.getBean(SnowflakeServer.class);
		snowflakeServer.start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				snowflakeServer.close();
			} catch (Exception e) {
			}
		}));

	}
}

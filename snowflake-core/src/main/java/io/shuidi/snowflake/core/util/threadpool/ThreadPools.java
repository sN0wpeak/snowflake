package io.shuidi.snowflake.core.util.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 13:33
 */
public final class ThreadPools {
	public static final ExecutorService sequenceExecutor = Executors.newCachedThreadPool();


	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				sequenceExecutor.shutdown();
			}
		}));
	}
}

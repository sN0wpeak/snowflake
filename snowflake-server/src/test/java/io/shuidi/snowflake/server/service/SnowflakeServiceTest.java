package io.shuidi.snowflake.server.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: Alvin Tian
 * Date: 2017/9/12 19:04
 */
@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SnowflakeServiceTest {


	@Autowired
	SnowflakeService snowflakeService;

	@Test
	public void generateId() throws Exception {
		int nThreads = 200;
		int size = 10000;
		CyclicBarrier cyclicBarrier = new CyclicBarrier(nThreads + 1);
		StopWatch stopWatch = new StopWatch();
		ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
		stopWatch.start();
		AtomicInteger atomicInteger = new AtomicInteger();
		for (int i = 0; i < nThreads; i++) {
			executorService.submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					cyclicBarrier.await();
					for (int j = 0; j < size; j++) {
						snowflakeService.generateId("account");
						atomicInteger.incrementAndGet();
					}
					cyclicBarrier.await();
					return null;
				}
			});
		}
		cyclicBarrier.await();
		cyclicBarrier.await();

		stopWatch.stop();
		System.out.println(String.format("[%.2f][%d][%.2f]", stopWatch.getTotalTimeSeconds(), atomicInteger.get(),
		                                 ((nThreads * size) / stopWatch.getTotalTimeSeconds())));
	}

	@Test
	public void generateId32() throws Exception {
		int nThreads = 200;
		int size = 10000;
		CyclicBarrier cyclicBarrier = new CyclicBarrier(nThreads + 1);
		StopWatch stopWatch = new StopWatch();
		ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
		stopWatch.start();
		AtomicInteger atomicInteger = new AtomicInteger();
		for (int i = 0; i < nThreads; i++) {
			executorService.submit(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					cyclicBarrier.await();
					for (int j = 0; j < size; j++) {
						snowflakeService.generateId32("account");
						atomicInteger.incrementAndGet();
					}
					cyclicBarrier.await();
					return null;
				}
			});
		}
		cyclicBarrier.await();
		cyclicBarrier.await();

		stopWatch.stop();
		System.out.println(String.format("[%.2f][%d][%.2f]", stopWatch.getTotalTimeSeconds(), atomicInteger.get(),
		                                 ((nThreads * size) / stopWatch.getTotalTimeSeconds())));
	}

}
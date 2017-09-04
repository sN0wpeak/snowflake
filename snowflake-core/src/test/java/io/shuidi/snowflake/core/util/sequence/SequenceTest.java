package io.shuidi.snowflake.core.util.sequence;

import com.google.common.collect.Lists;
import io.shuidi.snowflake.core.util.zk.CuratorFrameworkUtils;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 11:26
 */
public class SequenceTest {

	public static final int N_THREADS = 100;

	@Test
	public void get() throws Exception {

	}

	@Test
	public void set() throws Exception {
	}

	@Test
	public void incrementAndGet() throws Exception {
		int perSize = 10000;
		AtomicLong s3 = new AtomicLong();
		Sequence sequence = new Sequence(0, 1);
		ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
		CyclicBarrier cyclicBarrier = new CyclicBarrier(N_THREADS + 1);
		List<Future<Long>> futures = Lists.newArrayList();
		for (int i = 0; i < N_THREADS; i++) {
			futures.add(executorService.submit(() -> {
				long sum = 0;
				cyclicBarrier.await();
				for (int i1 = 0; i1 < perSize; i1++) {
					sum += sequence.incrementAndGet();
				}
				s3.addAndGet(sum);
				cyclicBarrier.await();
				return sum;
			}));
		}
		cyclicBarrier.await();
		cyclicBarrier.await();
		long s2 = 0;
		for (Future<Long> future : futures) {
			s2 += future.get();
		}
		long size = perSize * N_THREADS;
		long s1 = size * (size + 1) / 2;
		Assert.assertEquals(s2, s1);
		Assert.assertEquals(s1, s3.get());
		executorService.shutdownNow();
	}

	@Test
	public void addAndGet() throws Exception {
	}

}
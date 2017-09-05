package io.shuidi.snowflake.core.util.sequence;

import com.google.common.collect.Lists;
import io.shuidi.snowflake.core.util.threadpool.ThreadPools;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 11:25
 */
public class RangeSequenceTest extends TestSeqBase {

	@org.junit.Test
	public void incrementAndGetMulitMem() throws Exception {
		int perSize = 10;
		int threads = 10;
		int pros = 10;
		int increment = 10;
		RangeStore memoryRangeStore = new MemoryRangeStore(0, 10);
		testByStore(perSize, threads, pros, increment, memoryRangeStore);
//		Assert.assertEquals(s1, (s2) * (s2 + 1) / 2);
	}

	@org.junit.Test
	public void incrementAndGetMulitZk() throws Exception {
		int perSize = 10;
		int threads = 10;
		int pros = 10;
		int increment = 10;
		ZkRangeStore zkRangeStore = getZkRangeStore(0);
		testByStore(perSize, threads, pros, increment, zkRangeStore);
//		Assert.assertEquals(s1, (s2) * (s2 + 1) / 2);
	}


	public void testByStore(int perSize, int threads, int pros, int increment, RangeStore memoryRangeStore)
			throws InterruptedException, ExecutionException {
		long s1 = 0;
		List<Future<Long>> futures = new ArrayList<>();
		for (int i = 0; i < pros; i++) {
			futures.add(ThreadPools.sequenceExecutor.submit(() -> runRangeSeq(memoryRangeStore, threads, perSize, increment)));
		}
		for (Future<Long> future : futures) {
			s1 += future.get();
		}
		Assert.assertEquals(concurrentLinkedQueue.size(), concurrentLinkedQueue.stream().sorted().distinct().count());
		long s2 = perSize * threads * pros;
		Assert.assertEquals(s2, memoryRangeStore.getCurrRange() - (pros * increment));
		System.out.println(s1);
	}

	private ConcurrentLinkedQueue<Long> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();

	public long runRangeSeq(RangeStore memoryRangeStore, int threads, int perSize, int increment)
			throws InterruptedException, BrokenBarrierException, ExecutionException {
		RangeSequence rangeSequence = new RangeSequence(1, memoryRangeStore.getNextRange(), increment, memoryRangeStore);
		rangeSequence.start();

		AtomicLong s3 = new AtomicLong();
		ExecutorService executorService = Executors.newFixedThreadPool(threads);
		CyclicBarrier cyclicBarrier = new CyclicBarrier(threads + 1);
		List<Future<Long>> futures = Lists.newArrayList();
		ConcurrentLinkedQueue<Long> longs = new ConcurrentLinkedQueue<>();
		for (int i = 0; i < threads; i++) {
			futures.add(executorService.submit(() -> {
				long sum = 0;
				cyclicBarrier.await();
				for (int i1 = 0; i1 < perSize; i1++) {
					long v = rangeSequence.incrementAndGet();
					longs.add(v);
					sum += v;
					Thread.yield();
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
		rangeSequence.stop();
		Assert.assertEquals(longs.size(), longs.stream().sorted().distinct().count());
		Assert.assertEquals(s2, s3.get());
		Assert.assertEquals(threads * perSize, longs.size());
		executorService.shutdown();
		concurrentLinkedQueue.addAll(longs);
		return s2;
	}

	@Test
	public void incrementAndGet() throws Exception {
		MemoryRangeStore memoryRangeStore = new MemoryRangeStore(0, 10);
		RangeSequence rangeSequence = new RangeSequence(1, memoryRangeStore.getNextRange(), 10, memoryRangeStore);
		rangeSequence.start();
		long size = 10000;
		long s2 = 0;
		for (int i = 0; i < size; i++) {
			s2 += rangeSequence.incrementAndGet();
		}
		long s1 = size * (size + 1) / 2;
		Assert.assertEquals(s2, s1);
	}

	@Test
	public void incrementAndGetZk() throws Exception {
//		RangeStore memoryRangeStore = getZkRangeStore();
//		RangeSequence rangeSequence = new RangeSequence(1, memoryRangeStore.getNextRange(), 10, memoryRangeStore);
//		rangeSequence.start();
//		long size = 10000;
//		long s2 = 0;
//		for (int i = 0; i < size; i++) {
//			s2 += rangeSequence.incrementAndGet();
//		}
//		long s1 = size * (size + 1) / 2;
//		Assert.assertEquals(s2, s1);

		for (int i = 0; i < clients.size(); i++) {
			int finalI = i;
			ThreadPools.sequenceExecutor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						runRangeSeq(getZkRangeStore(finalI), 100, 100, 10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
			});
		}
		Thread.sleep(1000);
		ThreadPools.sequenceExecutor.shutdownNow();
		ThreadPools.sequenceExecutor.awaitTermination(1, TimeUnit.HOURS);

	}

	@Test
	public void testExchanger() throws InterruptedException {
		Exchanger<Integer> exchanger = new Exchanger<>();
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					System.out.println(exchanger.exchange(1));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(runnable);
		thread.start();
		Thread.sleep(100);
		thread.interrupt();
		exchanger.exchange(1);
	}

	@Test
	public void testInterr() throws Exception {
		RangeStore rangeStore = getZkRangeStore(0);
		RangeSequence rangeSequence = new RangeSequence(1, rangeStore.getNextRange(), 10, rangeStore);
		rangeSequence.start();
		Thread thread = new Thread(() -> {
			for (int j = 0; j < 100; j++) {
				System.out.println(rangeSequence.incrementAndGet());
			}
		});
		thread.start();
		Thread.sleep(10);
		rangeSequence.stop();
		thread.interrupt();
		thread.join();
	}
}
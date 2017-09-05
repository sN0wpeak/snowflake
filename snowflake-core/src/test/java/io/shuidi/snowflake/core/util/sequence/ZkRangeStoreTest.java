package io.shuidi.snowflake.core.util.sequence;

import io.shuidi.snowflake.core.util.threadpool.ThreadPools;
import org.junit.Test;

/**
 * Author: Alvin Tian
 * Date: 2017/9/4 19:37
 */
public class ZkRangeStoreTest extends TestSeqBase {

	@Test
	public void getNextRange() throws Exception {
		for (int i = 0; i < 100; i++) {
			ZkRangeStore zkRangeStore = getZkRangeStore(i % 3);
			new Thread(() -> {
				for (int j = 0; j < 100; j++) {
					try {
						System.out.println(zkRangeStore.getNextRange());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}

	}

	@Test
	public void testInterr() throws Exception {
		ZkRangeStore zkRangeStore = getZkRangeStore(0);
		Thread thread = new Thread(() -> {
			for (int j = 0; j < 100; j++) {
				try {
					if(zkRangeStore.getNextRange() == -2){
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		Thread.sleep(100);
		thread.interrupt();
		thread.join();
	}

}
package io.shuidi.snowflake.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.shuidi.snowflake.server.server.SnowflakeServer;
import org.junit.Test;

import java.util.Set;

import static io.shuidi.snowflake.server.server.SnowflakeServer.ALL_WORKER_IDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Author: Alvin Tian
 * Date: 2017/8/28 22:48
 */

public class SnowflakeServerTest {


	@Test
	public void testAllocWorkerId() throws Exception {
		SnowflakeServer snowflakeServer = mock(SnowflakeServer.class);
		when(snowflakeServer.peers()).thenReturn(
				ImmutableMap.of(0, new SnowflakeServer.Peer("", 1),
				                2, new SnowflakeServer.Peer("", 1))
		);
		when(snowflakeServer.allocWorkerId()).thenCallRealMethod();
		assertEquals(snowflakeServer.allocWorkerId(), 1);
	}

	@Test
	public void testDiff() {
		Set<Integer> diff = Sets.difference(ALL_WORKER_IDS, Sets.newHashSet(0, 1, 2, 3, 4));
		System.out.println(diff.stream().findFirst().get());
	}

}

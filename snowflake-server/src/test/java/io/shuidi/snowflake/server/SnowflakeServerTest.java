package io.shuidi.snowflake.server;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.Set;

import static io.shuidi.snowflake.server.SnowflakeServer.ALL_WORKER_IDS;

/**
 * Author: Alvin Tian
 * Date: 2017/8/28 22:48
 */
@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SnowflakeServerTest {
	@Autowired
	SnowflakeServer snowflakeServer;
	@Value("${server.port}")
	private int port;

	@Before
	public void setUp() throws Exception {
//		snowflakeServer.start();
	}


	@Test
	public void testDiff() {
		Set<Integer> diff = Sets.difference(ALL_WORKER_IDS, Sets.newHashSet(0, 1, 2, 3, 4));
		System.out.println(diff.stream().findFirst().get());
	}

	@Test
	public void testhasLeader() throws Exception {
		Assert.assertTrue(snowflakeServer.hasLeader());
	}

	@Test
	public void testGetLeaderUrl() throws Exception {
		Assert.assertEquals(snowflakeServer.getLeaderUrl(), snowflakeServer.getHostname() + ":" + port);
	}

	@Test
	public void testPeers() throws Exception {
		Map<Integer, SnowflakeServer.Peer> integerPeerMap = snowflakeServer.peers();
		Assert.assertTrue(integerPeerMap.keySet().size() == 1);
		Assert.assertTrue(integerPeerMap.keySet().contains(0));
		Assert.assertEquals(integerPeerMap.get(0).getHostUrl(), snowflakeServer.getLeaderUrl());
	}

	@Test
	public void testAllocWorkerId() throws Exception {
		Assert.assertTrue(snowflakeServer.allocWorkerId() == 1);
		Assert.assertFalse(snowflakeServer.allocWorkerId() == 1);
	}

	@Test
	public void testReleaseLeader() throws Exception {
		Assert.assertTrue(snowflakeServer.isLeader());
		snowflakeServer.releaseLeader();
		snowflakeServer.close();
		Assert.assertFalse(snowflakeServer.isLeader());
	}

	@Test
	public void testCloseStart() throws Exception {
//		snowflakeServer.close();
//		snowflakeServer.start();
//		snowflakeServer.close();
//		snowflakeServer.start();
	}

	@Test
	public void testSanityCheckPeers() throws Exception {
		snowflakeServer.sanityCheckPeers();
	}


}

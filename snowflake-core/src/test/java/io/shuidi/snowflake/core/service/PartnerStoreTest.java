package io.shuidi.snowflake.core.service;

import io.shuidi.snowflake.core.util.RetryRunner;
import io.shuidi.snowflake.core.util.zk.ZkUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.StringUtils;

import java.io.StringReader;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Author: Alvin Tian
 * Date: 2017/9/6 15:15
 */

public class PartnerStoreTest {
	CuratorFramework client;
	private String partnerStorePath = "/snowflake/store";

	@Before
	public void setUp() {
		client = ZkUtils.create("127.0.0.1:2181", 1000, 10000);
		client.start();
	}

	@After
	public void close() {
		CloseableUtils.closeQuietly(client);
	}

	@Test
	public void addPartner() throws Exception {
		PartnerStore partnerStore = new PartnerStore(client, partnerStorePath);
		Partner partner = new Partner();
		partner.setPartnerKey("test");
		partnerStore.addPartner("test", partner);
	}

	@Test
	public void removePartner() throws Exception {
	}

	@Test
	public void clear() throws Exception {
		RetryRunner.create()
		           .excludeExceptions(KeeperException.NoNodeException.class)
		           .includeExceptions(KeeperException.class)
		           .run((Callable<Void>) () -> {
			           client.delete().deletingChildrenIfNeeded().forPath(partnerStorePath);
			           return null;
		           });
	}

	@Test
	public void handlePartner() throws Exception {
		clear();
		PartnerStore partnerStore = new PartnerStore(client, partnerStorePath);
		partnerStore.init();
		partnerStore.handlePartner();
		Scanner scanner = new Scanner(System.in);
		int i = 0;
		while (scanner.hasNextLine()) {

			String line = scanner.nextLine();
			String action = "";
			StringReader stringReader = new StringReader(line);
			char c;
			while ((int) (c = (char) stringReader.read()) != -1) {
				if (c != ' ') {
					action += c;
				} else {
					if (!StringUtils.isEmpty(action)) {
						break;
					}
				}
			}
			String value = "";
			while ((int) (c = (char) stringReader.read()) != '\uFFFF') {
				if (c != ' ') {
					value += c;
				} else {
					if (!StringUtils.isEmpty(value)) {
						break;
					}
				}
			}
			action = action.toLowerCase();
			System.out.println("ACTION: " + action + " VALUE: " + value);

			switch (action) {
				case "add":
					Partner partner = new Partner();
					partner.setPartnerKey(value);
					partner.setStart(1000);
					partner.setRangeCount(1000);
					partner.setUseAuth(true);
					partnerStore.addPartner(value, partner);
					break;
				case "quit":
					System.out.println("byebye...");
					System.exit(0);
					break;
				case "del":
					partnerStore.removePartner(value);
					break;
			}

		}
	}

	public static void main(String[] args) throws Exception {
		PartnerStoreTest partnerStoreTest = new PartnerStoreTest();
		partnerStoreTest.setUp();
		partnerStoreTest.handlePartner();
	}


}
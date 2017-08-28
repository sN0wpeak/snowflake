package io.shuidi.snowflake.server.server;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 18:33
 */
public class Peer {
	private String host;
	private int port;

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public Peer(String host, int port) {

		this.host = host;
		this.port = port;
	}
}

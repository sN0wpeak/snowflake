package io.shuidi.snowflake.server.exception;

/**
 * Author: Alvin Tian
 * Date: 2017/8/29 15:35
 */
public class NotHaveLeaderException extends RuntimeException {
	public NotHaveLeaderException(String message) {
		super(message);
	}
}

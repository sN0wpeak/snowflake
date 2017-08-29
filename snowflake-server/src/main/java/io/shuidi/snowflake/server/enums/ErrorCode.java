package io.shuidi.snowflake.server.enums;

/**
 * Author: Alvin Tian
 * Date: 2017/8/28 20:20
 */
public enum ErrorCode {
	NOT_LEADER(-2, "当前无分配权限"),
	NOT_MORE_WORKER_ID(-3, "没有足够的workerId"),
	;

	private int code;
	private String desc;

	public int getCode() {
		return code;
	}

	public String getDesc() {
		return desc;
	}

	ErrorCode(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public static ErrorCode valueOfCode(int code) {
		for (ErrorCode e : values()) {
			if (e.getCode() == code) {
				return e;
			}
		}
		return null;
	}
}


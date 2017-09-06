package io.shuidi.snowflake.core.error.enums;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Author: Alvin Tian
 * Date: 2017/8/28 20:20
 */
public enum ErrorCode {

	AUTHENTICATION_FAILURE(4, "授权失败！"),
	SYSTEM_PARAM_ERROR(3, "参数错误"),
	SYSTEM_STOP(2, "系统关系!"),
	SYSTEM_ERROR(1, "系统异常!"),
	SNOW_NOT_A_LEADER(10002, "当前节点不是LEADER"),
	SNOW_NOT_MORE_WORKER_ID(10001, "没有足够的workerId"),
	SNOW_GET_NEXT_SEQUENCE_ERROR(10003, "获取该序列异常!");

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

	static Map<Integer, ErrorCode> errorCodeMap = Maps.newHashMap();

	static {
		for (ErrorCode errorCode : values()) {
			if (errorCodeMap.put(errorCode.code, errorCode) != null) {
				throw new IllegalStateException("ErrorCode " + errorCode.code + " 重复！");
			}
		}
	}
}


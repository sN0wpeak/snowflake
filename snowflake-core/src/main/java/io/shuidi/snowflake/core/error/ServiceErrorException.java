package io.shuidi.snowflake.core.error;

import io.shuidi.snowflake.core.error.enums.ErrorCode;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 11:12
 */
public class ServiceErrorException extends RuntimeException {

	private ErrorCode errorCode;

	public ServiceErrorException(ErrorCode errorCode) {
		super(errorCode.getDesc());
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public int getCode() {
		return errorCode.getCode();
	}

	public String msg() {
		return errorCode.getDesc();
	}

}

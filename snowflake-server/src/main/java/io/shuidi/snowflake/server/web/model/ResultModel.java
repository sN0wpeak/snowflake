package io.shuidi.snowflake.server.web.model;

import java.io.Serializable;

public class ResultModel implements Serializable {

	private int code = 0;
	private String msg = "";
	private Object data;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}

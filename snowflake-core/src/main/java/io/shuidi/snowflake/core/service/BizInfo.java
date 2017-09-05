package io.shuidi.snowflake.core.service;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 15:02
 */
public class BizInfo {

	@Min(0)
	private Integer start;
	private String appKey;
	private String appSecret;
	@Min(1000)
	private Integer rangeCount;

	public Integer getRangeCount() {
		return rangeCount;
	}

	public void setRangeCount(Integer rangeCount) {
		this.rangeCount = rangeCount;
	}
	@NotNull
	private boolean useAuth;

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}

	public boolean isUseAuth() {
		return useAuth;
	}

	public void setUseAuth(boolean useAuth) {
		this.useAuth = useAuth;
	}

	@Override
	public String toString() {
		return "BizInfo{" +
		       "start=" + start +
		       ", appKey='" + appKey + '\'' +
		       ", appSecret='" + appSecret + '\'' +
		       ", rangeCount=" + rangeCount +
		       ", useAuth=" + useAuth +
		       '}';
	}
}

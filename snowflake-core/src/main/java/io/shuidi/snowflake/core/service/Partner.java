package io.shuidi.snowflake.core.service;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Author: Alvin Tian
 * Date: 2017/9/5 15:02
 */
public class Partner {

	@Min(0)
	private Integer start;
	private String partnerKey;
	private String partnerSecret;
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

	public String getPartnerKey() {
		return partnerKey;
	}

	public void setPartnerKey(String partnerKey) {
		this.partnerKey = partnerKey;
	}

	public String getPartnerSecret() {
		return partnerSecret;
	}

	public void setPartnerSecret(String partnerSecret) {
		this.partnerSecret = partnerSecret;
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
		       ", partnerKey='" + partnerKey + '\'' +
		       ", partnerSecret='" + partnerSecret + '\'' +
		       ", rangeCount=" + rangeCount +
		       ", useAuth=" + useAuth +
		       '}';
	}
}

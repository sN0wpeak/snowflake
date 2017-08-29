package io.shuidi.snowflake.server.web.model;


/**
 * 暂无描述
 * Author: taoming
 * Date: 2015-03-25 10:56
 */
public class ResultResolver {
	public static ResultModel sendNormalResult(Object data) {
		ResultModel resultModel = new ResultModel();
		resultModel.setData(data);
		return resultModel;
	}
}


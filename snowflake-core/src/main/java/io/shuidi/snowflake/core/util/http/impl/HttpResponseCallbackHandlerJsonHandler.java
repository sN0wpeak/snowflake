package io.shuidi.snowflake.core.util.http.impl;

import com.alibaba.fastjson.JSONObject;
import io.shuidi.snowflake.core.util.http.HttpResponseCallbackHandler;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created by knightliao on 16/1/7.
 */
public class HttpResponseCallbackHandlerJsonHandler<T> implements HttpResponseCallbackHandler<T> {

	private Class<T> clazz = null;

	public HttpResponseCallbackHandlerJsonHandler(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public T handleResponse(String requestBody, HttpEntity entity) throws IOException {

		String json = EntityUtils.toString(entity, "UTF-8");
		return JSONObject.parseObject(json, clazz);
	}
}

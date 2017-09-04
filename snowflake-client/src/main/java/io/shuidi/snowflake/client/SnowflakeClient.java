package io.shuidi.snowflake.client;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import io.shuidi.snowflake.core.util.RetryRunner;
import io.shuidi.snowflake.core.util.http.HttpClientUtil;
import io.shuidi.snowflake.core.util.http.impl.HttpResponseCallbackHandlerJsonHandler;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Author: Alvin Tian
 * Date: 2017/8/29 15:42
 */
public class SnowflakeClient {

	private static Logger LOGGER = LoggerFactory.getLogger(SnowflakeClient.class);

	public int allocWorkerId(String serverHost) {
		Preconditions.checkNotNull(serverHost);
		return RetryRunner.create().addTryExceptions(IOException.class).thenThrow().run(() -> {
			String path = "/api/snowflake/get-alloc-workerid";
			String url = "http://" + serverHost + "/" + path;
			JSONObject result =
					HttpClientUtil.execute(new HttpGet(url), new HttpResponseCallbackHandlerJsonHandler<>(JSONObject.class));
			if (result.getInteger("code") == 0) {
				return result.getJSONObject("data").getInteger("workerId");
			} else {
				LOGGER.error("call allocWorkerId error {}", result.toJSONString());
				throw new IllegalStateException(result.getString("msg"));
			}
		});
	}

	public int getWorkerId(String serverHost) {
		Preconditions.checkNotNull(serverHost);
		return RetryRunner.create().addTryExceptions(IOException.class).thenThrow().run(() -> {
			String path = "/api/snowflake/get-worker-id";
			String url = "http://" + serverHost + "/" + path;
			JSONObject result =
					HttpClientUtil.execute(new HttpGet(url), new HttpResponseCallbackHandlerJsonHandler<>(JSONObject.class));
			if (result.getInteger("code") == 0) {
				return result.getJSONObject("data").getInteger("workerId");
			} else {
				LOGGER.error("call getWorkerId error {}", result.toJSONString());
				throw new IllegalStateException(result.getString("msg"));
			}
		});
	}

	public long getId(String serverHost, String query) {
		Preconditions.checkNotNull(serverHost);
		return RetryRunner.create().addTryExceptions(IOException.class).thenThrow().run(() -> {
			String path = "/api/snowflake/get-id";
			if (!StringUtils.isEmpty(query)) {
				path += "?" + query;
			}
			String url = "http://" + serverHost + "/" + path;
			JSONObject result =
					HttpClientUtil.execute(new HttpGet(url), new HttpResponseCallbackHandlerJsonHandler<>(JSONObject.class));
			if (result.getInteger("code") == 0) {
				return result.getJSONObject("data").getLongValue("id");
			} else {
				LOGGER.error("call getId error {}", result.toJSONString());
				throw new IllegalStateException(result.getString("msg"));
			}
		});
	}

	public int getId32(String serverHost, String query) {
		Preconditions.checkNotNull(serverHost);
		return RetryRunner.create().addTryExceptions(IOException.class).thenThrow().run(() -> {
			String path = "/api/snowflake/get-id32";
			if (!StringUtils.isEmpty(query)) {
				path += "?" + query;
			}
			String url = "http://" + serverHost + "/" + path;
			JSONObject result =
					HttpClientUtil.execute(new HttpGet(url), new HttpResponseCallbackHandlerJsonHandler<>(JSONObject.class));
			if (result.getInteger("code") == 0) {
				return result.getJSONObject("data").getInteger("id");
			} else {
				LOGGER.error("call getId error {}", result.toJSONString());
				throw new IllegalStateException(result.getString("msg"));
			}
		});
	}

	public String getReport(String serverHost) {
		Preconditions.checkNotNull(serverHost);
		return RetryRunner.create().addTryExceptions(IOException.class).thenThrow().run(() -> {
			String path = "/api/snowflake/report";
			String url = "http://" + serverHost + "/" + path;
			JSONObject result =
					HttpClientUtil.execute(new HttpGet(url), new HttpResponseCallbackHandlerJsonHandler<>(JSONObject.class));
			if (result.getInteger("code") == 0) {
				return result.getJSONObject("data").toJSONString();
			} else {
				LOGGER.error("call getReport error {}", result.toJSONString());
				throw new IllegalStateException(result.getString("msg"));
			}
		});
	}


}

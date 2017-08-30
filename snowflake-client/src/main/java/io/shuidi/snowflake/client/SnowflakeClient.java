package io.shuidi.snowflake.client;

import com.alibaba.fastjson.JSONObject;
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

	public int allocWorkerId(String serverHost) throws Exception {
		int tries = 0;
		while (true) {
			try {
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
			} catch (IOException e) {
				if (tries++ > 2) {
					throw e;
				} else {
					tries++;
					Thread.sleep(1000);
				}
			}
		}
	}

	public int getWorkerId(String serverHost) throws Exception {
		int tries = 0;
		while (true) {
			try {
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
			} catch (IOException e) {
				if (tries++ > 2) {
					throw e;
				} else {
					tries++;
					Thread.sleep(1000);
				}
			}
		}
	}

	public long getId(String serverHost, String query) throws Exception {
		int tries = 0;
		while (true) {
			try {

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
			} catch (IOException e) {
				if (tries++ > 2) {
					throw e;
				} else {
					tries++;
					Thread.sleep(1000);
				}
			}
		}
	}

	public String getReport(String serverHost) throws Exception {
		int tries = 0;
		while (true) {
			try {
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
			} catch (IOException e) {
				if (tries++ > 2) {
					throw e;
				} else {
					tries++;
					Thread.sleep(1000);
				}
			}
		}
	}


}

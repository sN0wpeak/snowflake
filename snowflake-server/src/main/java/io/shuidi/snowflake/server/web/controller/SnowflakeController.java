package io.shuidi.snowflake.server.web.controller;

import com.google.common.collect.ImmutableMap;
import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.server.enums.ErrorCode;
import io.shuidi.snowflake.server.service.SnowflakeService;
import io.shuidi.snowflake.server.web.model.ResultModel;
import io.shuidi.snowflake.server.web.model.ResultResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.regex.Pattern;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 17:04
 */

@Controller
@RequestMapping("/api/snowflake/")
public class SnowflakeController {

	@Autowired
	SnowflakeService snowflakeService;

	private Pattern agentParser = Pattern.compile("([a-zA-Z][a-zA-Z\\-0-9]*)");

	@RequestMapping(path = "/get-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public ResultModel getId(@RequestParam("useragent") String useragent) {
		if (!validUseragent(useragent)) {
			ResultModel resultModel = new ResultModel();
			resultModel.setCode(ErrorCode.NOT_AUTH_ERROR.getCode());
			resultModel.setMsg(ErrorCode.NOT_AUTH_ERROR.getDesc());
			ReporterHolder.incException(IllegalAccessError.class);
			return resultModel;
		}

		ResultModel resultModel = ResultResolver
				.sendNormalResult(ImmutableMap.of("id", snowflakeService.generateId()));

		ReporterHolder.metrics.counter("ids_generated").inc();
		ReporterHolder.metrics.counter("ids_generated_" + useragent).inc();

		return resultModel;
	}

	@RequestMapping(path = "/get-id32", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public ResultModel getId32(@RequestParam("useragent") String useragent) {
		if (!validUseragent(useragent)) {
			ResultModel resultModel = new ResultModel();
			resultModel.setCode(ErrorCode.NOT_AUTH_ERROR.getCode());
			resultModel.setMsg(ErrorCode.NOT_AUTH_ERROR.getDesc());
			ReporterHolder.incException(IllegalAccessError.class);
			return resultModel;
		}

		ResultModel resultModel = ResultResolver
				.sendNormalResult(ImmutableMap.of("id", snowflakeService.generateId32()));

		ReporterHolder.metrics.counter("ids_generated").inc();
		ReporterHolder.metrics.counter("ids_generated_" + useragent).inc();

		return resultModel;
	}

	public boolean validUseragent(String useragent) {
		if (StringUtils.isEmpty(useragent)) {
			return false;
		}
		return agentParser.matcher(useragent).matches();
	}

	@RequestMapping(path = "/get-worker-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public ResultModel getWorkId() {
		int workId = snowflakeService.getWorkerId();
		return ResultResolver.sendNormalResult(ImmutableMap.of("workerId", workId));
	}

	@RequestMapping(path = "/get-alloc-workerid", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public ResultModel getAllocId() throws Exception {
		ResultModel resultModel = new ResultModel();
		int workId = snowflakeService.allocWorkerId();
		if (workId < 0) {
			resultModel.setCode(workId);
			resultModel.setMsg(ErrorCode.valueOfCode(workId).getDesc());
			ReporterHolder.exceptionCounter.inc();
		} else {
			resultModel = ResultResolver.sendNormalResult(ImmutableMap.of("workerId", workId));
		}
		return resultModel;
	}

}

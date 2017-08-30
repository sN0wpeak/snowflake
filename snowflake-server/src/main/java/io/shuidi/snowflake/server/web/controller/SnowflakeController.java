package io.shuidi.snowflake.server.web.controller;

import com.google.common.collect.ImmutableMap;
import io.shuidi.snowflake.server.enums.ErrorCode;
import io.shuidi.snowflake.server.service.SnowflakeService;
import io.shuidi.snowflake.server.web.model.ResultModel;
import io.shuidi.snowflake.server.web.model.ResultResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 17:04
 */

@Controller
@RequestMapping("/api/snowflake/")
public class SnowflakeController {

	@Autowired
	SnowflakeService snowflakeService;

	@RequestMapping(path = "/get-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public ResultModel getId() {
		return ResultResolver
				.sendNormalResult(ImmutableMap.of("id", snowflakeService.generateId()));
	}

	@RequestMapping(path = "/get-worker-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public ResultModel getWorkId() {
		ResultModel resultModel = new ResultModel();
		int workId = snowflakeService.getWorkerId();
		if (workId < 0) {
			resultModel.setCode(workId);
			resultModel.setMsg(ErrorCode.valueOfCode(workId).getDesc());
		} else {
			resultModel = ResultResolver.sendNormalResult(ImmutableMap.of("workerId", workId));
		}
		return resultModel;
	}

	@RequestMapping(path = "/get-alloc-workerid", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public ResultModel getAllocId() throws Exception {
		ResultModel resultModel = new ResultModel();
		int workId = snowflakeService.allocWorkerId();
		if (workId < 0) {
			resultModel.setCode(workId);
			resultModel.setMsg(ErrorCode.valueOfCode(workId).getDesc());
		} else {
			resultModel = ResultResolver.sendNormalResult(ImmutableMap.of("workerId", workId));
		}
		return resultModel;
	}
}

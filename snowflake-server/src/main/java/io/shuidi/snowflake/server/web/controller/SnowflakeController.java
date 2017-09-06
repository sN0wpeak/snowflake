package io.shuidi.snowflake.server.web.controller;

import com.google.common.collect.ImmutableMap;
import io.shuidi.snowflake.core.error.ServiceErrorException;
import io.shuidi.snowflake.core.error.enums.ErrorCode;
import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.core.service.PartnerStoreHolder;
import io.shuidi.snowflake.core.service.Partner;
import io.shuidi.snowflake.server.annotation.PartnerKeyRequire;
import io.shuidi.snowflake.server.service.SnowflakeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

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
	@PartnerKeyRequire
	public Map<String, Long> getId(@RequestParam("partnerKey") String partnerKey) {
		long id = snowflakeService.generateId(partnerKey);
		ReporterHolder.metrics.counter("ids_generated").inc();
		ReporterHolder.metrics.counter("ids_generated_" + partnerKey).inc();
		return ImmutableMap.of("id", id);
	}

	@RequestMapping(path = "/get-id32", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	@PartnerKeyRequire
	public Map<String, Integer> getId32(@RequestParam("partnerKey") String partnerKey) {
		int id = snowflakeService.generateId32(partnerKey);
		ReporterHolder.metrics.counter("ids_generated").inc();
		ReporterHolder.metrics.counter("ids_generated_" + partnerKey).inc();
		return ImmutableMap.of("id", id);
	}


	@RequestMapping(path = "/get-worker-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public Map<String, Integer> getWorkId() {
		int workId = snowflakeService.getWorkerId();
		return ImmutableMap.of("workerId", workId);
	}

	@RequestMapping(path = "/get-alloc-workerid", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public Map<String, Integer> getAllocId() throws Exception {
		int workId = snowflakeService.allocWorkerId();
		return ImmutableMap.of("workerId", workId);
	}

	@RequestMapping(path = "/add-biz", method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
	@ResponseBody
	public String addBiz(@RequestBody @Valid Partner bizInfo, BindingResult result) throws Exception {
		if (result.hasErrors()) {
			throw new ServiceErrorException(ErrorCode.SYSTEM_PARAM_ERROR);
		}
		PartnerStoreHolder.getBizStore().addPartner(bizInfo.getPartnerKey(), bizInfo);
		return "ok";
	}


}

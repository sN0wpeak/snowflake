package io.shuidi.snowflake.server.controller;

import io.shuidi.snowflake.core.service.impl.SnowflakeIDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Author: Alvin Tian
 * Date: 2017/8/23 17:04
 */

@Controller
@RequestMapping("/api/snowflake/")
public class IDController {

	@Autowired
	SnowflakeIDGenerator snowflakeIDGenerator;

	@RequestMapping(path = "/get-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	public long getId() {
		return snowflakeIDGenerator.generateId();
	}

	@RequestMapping(path = "/get-work-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	public long getWorkId() {
		return snowflakeIDGenerator.generateId();
	}

	@RequestMapping(path = "/get-fetch-id", method = {RequestMethod.GET}, produces = {"application/json;charset=UTF-8"})
	public long getFetchId() {
		return snowflakeIDGenerator.generateId();
	}
}

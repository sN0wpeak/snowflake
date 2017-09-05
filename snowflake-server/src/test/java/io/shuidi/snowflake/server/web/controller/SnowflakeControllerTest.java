package io.shuidi.snowflake.server.web.controller;

import io.shuidi.snowflake.server.SnowflakeServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Author: Alvin Tian
 * Date: 2017/8/29 19:59
 */
@RunWith(SpringRunner.class)
@SpringBootConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SnowflakeControllerTest {

	private MockMvc mockMvc;
	@Autowired
	private WebApplicationContext wac;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	@Autowired
	SnowflakeServer snowflakeServer;

	@Before
	public void setUp() throws Exception {
		setContext();
		this.mockMvc = webAppContextSetup(this.wac).build();
		request = new MockHttpServletRequest();
		request.setCharacterEncoding("UTF-8");
		response = new MockHttpServletResponse();
	}

	@Test
	public void getId() throws Exception {
		mockMvc.perform(get("/api/snowflake/get-id?useragent=ACCOUNT").header("Content-Type", "application/json;charset=UTF-8"))
		       .andDo(MockMvcResultHandlers.print())
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.code").value(0))
		       .andExpect(jsonPath("$.data.id").isNumber());
		mockMvc.perform(get("/api/snowflake/report").header("Content-Type", "application/json;charset=UTF-8"))
		       .andDo(MockMvcResultHandlers.print());
	}


	private void setContext() throws Exception {
//		try {
//			snowflakeServer.close();
//			snowflakeServer.start();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	@Test
	public void getWorkId() throws Exception {
		mockMvc.perform(get("/api/snowflake/get-worker-id").header("Content-Type", "application/json;charset=UTF-8"))
		       .andDo(MockMvcResultHandlers.print())
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.code").value(0))
		       .andExpect(jsonPath("$.data.workerId").value(0));
	}

	@Test
	public void getAllocId() throws Exception {
		mockMvc.perform(get("/api/snowflake/get-alloc-workerid").header("Content-Type", "application/json;charset=UTF-8"))
		       .andDo(MockMvcResultHandlers.print())
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.code").value(0))
		       .andExpect(jsonPath("$.data.workerId").value(1));
	}

	@Test
	public void getIdWhenNotLeader() throws Exception {
		snowflakeServer.releaseLeader();
		mockMvc.perform(get("/api/snowflake/get-id?useragent=ACCOUNT").header("Content-Type", "application/json;charset=UTF-8"))
		       .andDo(MockMvcResultHandlers.print())
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.code").value(0))
		       .andExpect(jsonPath("$.data.id").isNumber());

	}

	@Test
	public void getWorkIdWhenNotLeader() throws Exception {
		snowflakeServer.releaseLeader();
		mockMvc.perform(get("/api/snowflake/get-worker-id").header("Content-Type", "application/json;charset=UTF-8"))
		       .andDo(MockMvcResultHandlers.print())
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.code").value(0))
		       .andExpect(jsonPath("$.data.workerId").value(0));
	}

	@Test
	public void getAllocIdWhenNotLeader() throws Exception {
		snowflakeServer.releaseLeader();
		mockMvc.perform(get("/api/snowflake/get-alloc-workerid").header("Content-Type", "application/json;charset=UTF-8"))
		       .andDo(MockMvcResultHandlers.print())
		       .andExpect(status().isOk())
		       .andExpect(jsonPath("$.code").value(-2))
		       .andExpect(jsonPath("$.data.workerId").doesNotExist());
	}




}
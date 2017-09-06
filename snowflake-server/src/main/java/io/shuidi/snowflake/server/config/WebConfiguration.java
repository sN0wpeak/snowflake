package io.shuidi.snowflake.server.config;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter4;
import io.shuidi.snowflake.core.error.ServiceErrorException;
import io.shuidi.snowflake.core.error.enums.ErrorCode;
import io.shuidi.snowflake.core.report.ReporterHolder;
import io.shuidi.snowflake.core.service.PartnerStoreHolder;
import io.shuidi.snowflake.server.annotation.PartnerKeyRequire;
import io.shuidi.snowflake.server.web.model.ResultModel;
import io.shuidi.snowflake.server.web.model.ResultResolver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Author: Alvin Tian
 * Date: 2017/8/28 22:11
 */
@Configuration
public class WebConfiguration extends WebMvcConfigurationSupport {
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(customJackson2HttpMessageConverter());
		super.configureMessageConverters(converters);
	}

	@Override
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		exceptionResolvers.add(customHandlerExceptionResolver());
		super.configureHandlerExceptionResolvers(exceptionResolvers);
	}

	@Override
	protected void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(partnerkeyHandlerInterceptor());
		super.addInterceptors(registry);
	}


	@Bean
	public HandlerInterceptorAdapter partnerkeyHandlerInterceptor() {
		return new HandlerInterceptorAdapter() {
			private Pattern agentParser = Pattern.compile("([a-zA-Z][a-zA-Z\\-0-9]*)");

			public PartnerKeyRequire getAuthpartnerKey(Object handler) {
				if (handler instanceof HandlerMethod) {
					return ((HandlerMethod) handler).getMethodAnnotation(PartnerKeyRequire.class);
				} else {
					return null;
				}
			}

			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
				PartnerKeyRequire authentication = getAuthpartnerKey(handler);
				if (authentication == null) {
					return true;
				}
				if (!validpartnerKey(request.getParameter("partnerKey"))) {
					throw new ServiceErrorException(ErrorCode.AUTHENTICATION_FAILURE);
				}
				return true;
			}

			public boolean validpartnerKey(String partnerKey) {
				if (StringUtils.isEmpty(partnerKey)) {
					return false;
				}
				if (agentParser.matcher(partnerKey).matches()) {
					if (PartnerStoreHolder.getBizStore().getPartner(partnerKey) != null) {
						return true;
					}
				}
				return false;
			}
		};
	}

	@Bean
	public HandlerExceptionResolver customHandlerExceptionResolver() {

		return new AbstractHandlerExceptionResolver() {
			private Logger LOGGER = LoggerFactory.getLogger(HandlerInterceptorAdapter.class);

			@Override
			protected ModelAndView doResolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
			                                          Object o,
			                                          Exception e) {
				ModelAndView modelAndView = new ModelAndView();
				ResultModel resultModel = new ResultModel();

				if (e instanceof ServiceErrorException) {
					ServiceErrorException serviceErrorException = (ServiceErrorException) e;
					resultModel.setCode(serviceErrorException.getCode());
					resultModel.setMsg(serviceErrorException.getMessage());
//					LOGGER.error(serviceErrorException.getMessage());
				} else {
					resultModel.setCode(ErrorCode.SYSTEM_ERROR.getCode());
					resultModel.setMsg(ErrorCode.SYSTEM_ERROR.getDesc());

				}
				LOGGER.error("error", e);
				ReporterHolder.incException(String.format("%s.%d", e.getClass().getSimpleName(), resultModel.getCode()));
				printJson(httpServletResponse, resultModel);
				return modelAndView;
			}


			private void printJson(HttpServletResponse response, Object value) {
				response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
				try {
					PrintWriter writer = response.getWriter();
					writer.print(JSONObject.toJSONStringWithDateFormat(value, "yyyy-MM-dd HH:mm:ss"));
					writer.flush();
				} catch (IOException e) {
				}
			}
		};

	}


	@Bean
	public HttpMessageConverter customJackson2HttpMessageConverter() {
		return new FastJsonHttpMessageConverter4() {
			@Override
			protected void writeInternal(Object obj, Type type, HttpOutputMessage outputMessage)
					throws IOException, HttpMessageNotWritableException {
				if (obj instanceof ResultModel) {
					super.writeInternal(obj, type, outputMessage);
				} else {
					super.writeInternal(ResultResolver.sendNormalResult(obj), type, outputMessage);
				}
			}
		};
	}


}

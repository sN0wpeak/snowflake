package io.shuidi.snowflake.server.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter4;
import io.shuidi.snowflake.server.web.model.ResultModel;
import io.shuidi.snowflake.server.web.model.ResultResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

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

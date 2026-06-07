package org.hyw.tools.generator.web;

import java.util.Arrays;

import org.hyw.tools.generator.web.filter.JsonFormContentFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.util.StringUtils;

@SpringBootApplication
@EnableAsync
public class WebGenerator {
	private static final Logger logger = LoggerFactory.getLogger(WebGenerator.class);

	@Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,http://[::1]:*}")
	private String allowedOriginPatterns;

	public static void main(String[] args) {
		logger.info("正在启动代码生成器 Web 应用...");
		SpringApplication.run(WebGenerator.class, args);
		logger.info("代码生成器 Web 应用启动完成");
	}

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		logger.debug("配置 CORS 跨域过滤器");
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		for (String originPattern : StringUtils.commaDelimitedListToStringArray(allowedOriginPatterns)) {
			String trimmed = originPattern.trim();
			if (!trimmed.isEmpty()) {
				config.addAllowedOriginPattern(trimmed);
			}
		}
		config.setAllowedHeaders(Arrays.asList("Accept", "Content-Type", "X-Requested-With", "X-USER-TOKEN",
				"X-Generator-Client-Id"));
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setMaxAge(18000L);
		config.addExposedHeader("Content-Disposition");
		source.registerCorsConfiguration("/**", config);
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<CorsFilter>(new CorsFilter(source));
		bean.setOrder(0);
		logger.debug("CORS 跨域过滤器配置完成");
		return bean;
	}

	@Bean
	public FormContentFilter formContentFilter() {
		logger.debug("配置 JSON 表单内容过滤器");
		JsonFormContentFilter filter = new JsonFormContentFilter();
		logger.debug("JSON 表单内容过滤器配置完成");
		return filter;
	}

}

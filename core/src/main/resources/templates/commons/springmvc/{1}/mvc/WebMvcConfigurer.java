package ${mvcPackage};

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.alibaba.fastjson.serializer.NameFilter;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import ${interceptorPackage}.ContextInterceptor;

@Configuration
public class WebMvcConfigurer extends WebMvcConfigurationSupport { // WebMvcConfigurerAdapter {
	
	private static final Logger logger = LoggerFactory.getLogger(WebMvcConfigurer.class);
	
	@Autowired
	private ContextInterceptor contextInterceptor;
	
	#[[@Value("${spring.jackson.date-format:yyyy-MM-dd HH:mm:ss}")]]#
	private String dateFormat;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(contextInterceptor).addPathPatterns("/**").excludePathPatterns("/favicon.ico","/webjars/**","/swagger-resources/**");
		super.addInterceptors(registry);
	}
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
		registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/favicon.ico");
		registry.addResourceHandler("/swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
		registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
	}
#if("fastjson"=="${json_type}")	
	@Bean
    public FormContentFilter formContentFilter() {
        return new JsonFormContentFilter();
    }
#end

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		if(converters.isEmpty()) {
			super.addDefaultHttpMessageConverters(converters);
		}
#if("fastjson"=="${json_type}")
		converters.add(0, fastJsonHttpMessageConverter());
#end
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				// 设置jackson可读格式化 actuator输出硬编码采用jackson
				MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = (MappingJackson2HttpMessageConverter) converter;
				mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper());
			}
		}
	}

	/*
	 * @Override protected void addCorsMappings(CorsRegistry registry) {
	 * registry.addMapping("/**").allowedOrigins("*").allowedHeaders("*").
	 * allowCredentials(true).allowedMethods("*") .maxAge(18000L); }
	 */

	@Bean
	public FilterRegistrationBean corsFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		CorsConfiguration config = new CorsConfiguration();
		// 允许cookies跨域
		config.setAllowCredentials(true);
		// #允许向该服务器提交请求的URI，*表示全部允许，在SpringMVC中，如果设成*，会自动转成当前请求头中的Origin
		config.addAllowedOrigin("*");
		// #允许访问的头信息,*表示全部
		config.addAllowedHeader("*");
		// 预检请求的缓存时间（秒），即在这个时间段里，对于相同的跨域请求不会再预检了
		config.setMaxAge(18000L);
		// 允许提交请求的方法，*表示全部允许
		config.addAllowedMethod("*");
		source.registerCorsConfiguration("/**", config);
		FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
		bean.setOrder(0);
		return bean;
	}
	 
#if("fastjson"=="${json_type}")	
	public FastJsonHttpMessageConverter fastJsonHttpMessageConverter() {
		FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
		converter.setFastJsonConfig(fastJsonConfig());
		// 处理中文乱码问题
		List<MediaType> fastMediaTypes = new ArrayList<>();
		fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
		converter.setSupportedMediaTypes(fastMediaTypes);
		return converter;
	}

	@Bean
	public FastJsonConfig fastJsonConfig() {
		FastJsonConfig conf = new FastJsonConfig();
		// SerializerFeature.PrettyFormat,SerializerFeature.WriteMapNullValue,SerializerFeature.WriteNullStringAsEmpty,
		conf.setSerializerFeatures(SerializerFeature.WriteEnumUsingToString);
		conf.setDateFormat(dateFormat);
		conf.setCharset(Charset.forName("UTF-8"));
		// 解决Long转json精度丢失的问题
//		SerializeConfig serializeConfig = SerializeConfig.globalInstance;
//		serializeConfig.put(BigInteger.class, ToStringSerializer.instance);
//		serializeConfig.put(Long.class, ToStringSerializer.instance);
//		serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
//		conf.setSerializeConfig(serializeConfig);
#if("plus"=="$mapperType")	
//		NameFilter nameFilter = new NameFilter() {
//			@Override
//			public String process(Object object, String name, Object value) {
//				if (object instanceof Page) {
//					if ("current".equals(name)) {
//						return "pageNo";
//					}
//					if ("total".equals(name)) {
//						return "totalCount";
//					}
//				}
//				return name;
//			}
//		};
#end
		ValueFilter valueFilter = new ValueFilter() {
			@Override
			public Object process(Object object, String name, Object value) {
				if(null==value) {
					return value;
				}
				if (name.endsWith("Time") && StringUtils.length(value.toString()) == 13&& value.toString().matches("^\\d{13}$")) {
					String v = format(Long.valueOf(value.toString()).longValue(), dateFormat);
					logger.info("format {}:{}",name,v);
					return v;
				}
				return value;
			}
		};
		conf.setSerializeFilters(valueFilter);
		return conf;
	}
	
	public String format(long datetime, String pattern) {
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(datetime), ZoneId.systemDefault())
				.format(DateTimeFormatter.ofPattern(pattern));
	}
#end
	
	/**
	 * 设置jackson可读格式化
	 * 
	 * @author: heyiwu
	 * @return
	 */
	public ObjectMapper objectMapper() {
		ObjectMapper objMapper = new ObjectMapper();
		objMapper.enable(SerializationFeature.INDENT_OUTPUT);
		return objMapper;
	}
}

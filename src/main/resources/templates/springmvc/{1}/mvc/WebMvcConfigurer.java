package ${mvcPackage};

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.alibaba.fastjson.serializer.NameFilter;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonpHttpMessageConverter4;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ${interceptorPackage}.ContextInterceptor;

@Configuration
public class WebMvcConfigurer extends WebMvcConfigurationSupport {

	@Autowired
	private ContextInterceptor contextInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(contextInterceptor).addPathPatterns("/**");
		super.addInterceptors(registry);
	}

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
#if("fastjson"=="${json_type}")
		converters.add(fastJsonHttpMessageConverter());
#end
		converters.add(mappingJackson2HttpMessageConverter());
	}

	@Override
	protected void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").allowedOrigins("*").allowedHeaders("*").allowCredentials(true).allowedMethods("*")
				.maxAge(18000L);
	}

	/*
	 * @Bean public FilterRegistrationBean corsFilter() {
	 * UrlBasedCorsConfigurationSource source = new
	 * UrlBasedCorsConfigurationSource(); CorsConfiguration config = new
	 * CorsConfiguration(); // 允许cookies跨域 config.setAllowCredentials(true); //
	 * #允许向该服务器提交请求的URI，*表示全部允许，在SpringMVC中，如果设成*，会自动转成当前请求头中的Origin
	 * config.addAllowedOrigin("*"); // #允许访问的头信息,*表示全部
	 * config.addAllowedHeader("*"); // 预检请求的缓存时间（秒），即在这个时间段里，对于相同的跨域请求不会再预检了
	 * config.setMaxAge(18000L); // 允许提交请求的方法，*表示全部允许 config.addAllowedMethod("*");
	 * source.registerCorsConfiguration("/**", config); FilterRegistrationBean bean
	 * = new FilterRegistrationBean(new CorsFilter(source)); bean.setOrder(0);
	 * return bean; }
	 */
#if("fastjson"=="${json_type}")	
	public FastJsonpHttpMessageConverter4 fastJsonHttpMessageConverter() {
		FastJsonpHttpMessageConverter4 converter = new FastJsonpHttpMessageConverter4();
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
		conf.setSerializerFeatures(SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
				SerializerFeature.WriteEnumUsingToString);
	//	conf.setDateFormat("yyyy-MM-dd HH:mm:ss");
		conf.setCharset(Charset.forName("UTF-8"));
		// 解决Long转json精度丢失的问题
		SerializeConfig serializeConfig = SerializeConfig.globalInstance;
		serializeConfig.put(BigInteger.class, ToStringSerializer.instance);
		serializeConfig.put(Long.class, ToStringSerializer.instance);
		serializeConfig.put(Long.TYPE, ToStringSerializer.instance);
		conf.setSerializeConfig(serializeConfig);
#if("plus"=="$mapperType")	
		NameFilter nameFilter = new NameFilter() {
			@Override
			public String process(Object object, String name, Object value) {
				if (object instanceof Page) {
					if ("current".equals(name)) {
						return "pageNo";
					}
					if ("total".equals(name)) {
						return "totalCount";
					}
				}
				return name;
			}
		};
		conf.setSerializeFilters(nameFilter);
#end		
		return conf;
	}
#end
	/**
	 * 设置jackson可读格式化 actuator输出硬编码采用jackson
	 * 
	 * @author: heyiwu
	 * @return
	 */
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
		mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper());
		return mappingJackson2HttpMessageConverter;
	}

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

package ${rootPackage}.${projectName}.${moduleName};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
#if("fastjson"=="${json_type}")
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
#end
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.servlet.DispatcherServlet;

@SpringBootApplication
@ImportResource(locations={"classpath:/spring/*.xml"})
public class Booter{
	public static void main(String[] args ){
		//springloaded
		//	1. mvn spring-boot:run   
		//	2. vm args: -javaagent:%m2_repo%\org\springframework\springloaded\1.2.7.RELEASE\springloaded-1.2.7.RELEASE.jar -noverify
		//springboot devtools
		SpringApplication.run(Booter.class,args);
	}
	
#if("fastjson"=="${json_type}")	
	/**
	 * 1、extends WebMvcConfigurerAdapter覆写configureMessageConverters方法
	 * 2、注入FastJsonHttpMessageConverter
	 */
	@Bean
	public HttpMessageConverters fastJsonHttpMessageConverter(){
		FastJsonHttpMessageConverter converter= new FastJsonHttpMessageConverter();
		FastJsonConfig conf=new FastJsonConfig();
		conf.setSerializerFeatures(SerializerFeature.PrettyFormat #if($!{write_null_value}),SerializerFeature.WriteMapNullValue#end);
		converter.setFastJsonConfig(conf);
		return new HttpMessageConverters(converter);
	}
#end

	@Bean  
	public ServletRegistrationBean dispatcherRegistration(DispatcherServlet dispatcherServlet) {  
	    ServletRegistrationBean registration = new ServletRegistrationBean(  
	            dispatcherServlet);  
	    dispatcherServlet.setThrowExceptionIfNoHandlerFound(true);  
	    return registration;  
	}  
}
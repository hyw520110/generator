package ${rootPackage}.${projectName}#if($!{moduleName}).${moduleName}#end;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
#if("fastjson"=="${json_type}")
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonpHttpMessageConverter4;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
#end
import io.undertow.Undertow.Builder;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.DispatcherServlet;
import javax.validation.Validator;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@SpringBootApplication
@EnableCaching
@EnableAsync
#if("plus"!="$mapperType")@MapperScan("${mapperPackage}")#end
public class Booter{
    
	public static void main(String[] args ){
		//springloaded
		//	1. mvn spring-boot:run   
		//	2. vm args: -javaagent:%m2_repo%\org\springframework\springloaded\1.2.7.RELEASE\springloaded-1.2.7.RELEASE.jar -noverify
		//springboot devtools
	    //System.setProperty("spring.devtools.restart.enabled", "false");
#if($!{spring_boot_dubbo_version})
	    //设置dubbo日志适配
        System.setProperty("dubbo.application.logger", "slf4j");
        //关闭dubbo实时数据追踪
        //System.setProperty("dubbo.trace.enabled", "false");
		System.setProperty("spring.main.allow-bean-definition-overriding", "true");
#end
        SpringApplication.run(Booter.class,args);
	}
    
    /**
     * springmvc注册/初始化
     * @author:  heyiwu 
     * @param dispatcherServlet
     * @return
     */
	@Bean  
	public ServletRegistrationBean dispatcherRegistration(DispatcherServlet dispatcherServlet) {  
	    ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet);  
	    dispatcherServlet.setThrowExceptionIfNoHandlerFound(true); 
	    registration.setLoadOnStartup(2);
	    return registration;  
	}
	
	@Bean
    public ResourceBundleMessageSource messageSource() throws Exception {  
        ResourceBundleMessageSource rbms = new ResourceBundleMessageSource();  
        rbms.setDefaultEncoding("${encoding}");  
        rbms.setBasenames("conf/messages");  
        return rbms;  
    }  
  
    @Bean  
    public Validator validator() throws Exception {  
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();  
        validator.setValidationMessageSource(messageSource());  
        return validator;  
    } 

#if($ssl)
	/**
	 * tomcat ssl /http2https  
	 * @author:  heyiwu 
	 * @return
	 */
	@Bean
    public EmbeddedServletContainerFactory servletContainer() {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint constraint = new SecurityConstraint();
                constraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                constraint.addCollection(collection);
                context.addConstraint(constraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(httpConnector());
        return tomcat;
    }

    @Bean
    public Connector httpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        //Connector监听的http的端口号
        connector.setPort(${server_port});
        connector.setSecure(false);
        //监听到http的端口号后转向到的https的端口号
        connector.setRedirectPort(8443);
        return connector;
    }

#end

/*	@Bean
	public UndertowEmbeddedServletContainerFactory embeddedServletContainerFactory() {
	    UndertowEmbeddedServletContainerFactory factory = new UndertowEmbeddedServletContainerFactory();
	    factory.addBuilderCustomizers(new UndertowBuilderCustomizer() {
	        @Override
	        public void customize(Builder builder) {
	        	//多端口支持
	           // builder.addHttpListener(${server_port}, "0.0.0.0");
	        }
	    });
	    return factory;
	}*/
    
      
}
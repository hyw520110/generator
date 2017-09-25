package ${rootPackage}.${projectName}.${moduleName};

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
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
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

#if($!{spring_boot_dubbo_version})
import com.alibaba.boot.dubbo.annotation.EnableDubboConfiguration;
#end

import org.springframework.boot.context.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
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
#if($!{spring_boot_dubbo_version})@EnableDubboConfiguration
#end
@EnableAsync
public class Booter{
    @Value("${spring.messages.basename}")
    private String basenames;
    
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
#end
        SpringApplication.run(Booter.class,args);
	}
	
#if("fastjson"=="${json_type}")	
	/**
	 * 1、extends WebMvcConfigurerAdapter覆写configureMessageConverters方法
	 * 2、注入FastJsonHttpMessageConverter
	 */
	@Bean
	public HttpMessageConverters fastJsonHttpMessageConverter(){
		FastJsonpHttpMessageConverter4 converter= new FastJsonpHttpMessageConverter4();
		FastJsonConfig conf=converter.getFastJsonConfig();
		conf.setSerializerFeatures(SerializerFeature.PrettyFormat #if($!{write_null_value}),SerializerFeature.WriteMapNullValue#end);
		converter.setFastJsonConfig(conf);
		//处理中文乱码问题
        List<MediaType> fastMediaTypes = new ArrayList<>();
        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        converter.setSupportedMediaTypes(fastMediaTypes);
		return new HttpMessageConverters(converter,mappingJackson2HttpMessageConverter());
	}
#end
     
    /**
     * 设置jackson可读格式化
     * actuator输出硬编码采用jackson
     * @author:  heyiwu 
     * @return
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper());
        return mappingJackson2HttpMessageConverter;
    }
    /**
     * 设置jackson可读格式化
     * @author:  heyiwu 
     * @return
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objMapper = new ObjectMapper();
        objMapper.enable(SerializationFeature.INDENT_OUTPUT);
        return objMapper;
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
        rbms.setBasenames(basenames);  
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
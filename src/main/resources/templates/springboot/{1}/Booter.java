package ${rootPackage}.${projectName}.${moduleName};

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
#if("fastjson"=="${json_type}")
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.MediaType;

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonpHttpMessageConverter4;
#end
import io.undertow.Undertow.Builder;
import org.springframework.boot.context.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.DispatcherServlet;

@SpringBootApplication
@ImportResource(locations={"classpath:/spring/*.xml"})
@EnableConfigurationProperties
@EnableCaching
public class Booter{
    @Value("${druid.stat.urlMappings}")
    private String druidStatUrlMappings;

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
		FastJsonpHttpMessageConverter4 converter= new FastJsonpHttpMessageConverter4();
		FastJsonConfig conf=converter.getFastJsonConfig();
		conf.setSerializerFeatures(SerializerFeature.PrettyFormat #if($!{write_null_value}),SerializerFeature.WriteMapNullValue#end);
		converter.setFastJsonConfig(conf);
		//处理中文乱码问题
        List<MediaType> fastMediaTypes = new ArrayList<>();
        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        converter.setSupportedMediaTypes(fastMediaTypes);
		return new HttpMessageConverters(converter);
	}
#end

	@Bean  
	public ServletRegistrationBean dispatcherRegistration(DispatcherServlet dispatcherServlet) {  
	    ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet);  
	    dispatcherServlet.setThrowExceptionIfNoHandlerFound(true); 
	    registration.setLoadOnStartup(2);
	    return registration;  
	}

#if($ssl)
	//tomcat ssl /http2https	
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

    @Bean
    public FilterRegistrationBean druidWebStatFilter() {
        FilterRegistrationBean frb = new FilterRegistrationBean(new WebStatFilter());
        List<String> url = new ArrayList<>();
        url.add("/*");
        frb.setUrlPatterns(url);
        frb.setInitParameters(druidWebStatInitParameters());
        return frb;
    }
    
    @Bean
    public ServletRegistrationBean druidStatViewServlet() {
        ServletRegistrationBean srb = new ServletRegistrationBean(new StatViewServlet(), druidStatUrlMappings);
        srb.setInitParameters(druidStatInitParameters());
        srb.setLoadOnStartup(1);
        return srb;
    }
    
    @Bean
    @ConfigurationProperties(prefix = "druid.stat.initParameters")
    public Map<String, String> druidStatInitParameters() {
        return new HashMap<String, String>();
    }
    
    @Bean
    @ConfigurationProperties(prefix = "druid.web-stat.initParameters")
    public Map<String, String> druidWebStatInitParameters() {
        return new HashMap<String, String>();
    }
}
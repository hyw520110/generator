package  ${druidPackage};

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.alibaba.druid.filter.logging.Slf4jLogFilter;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;

@Configuration("dataSource")
@ConfigurationProperties(prefix = "druid")
public class DruidDataSource extends com.alibaba.druid.pool.DruidDataSource {

    private static final long serialVersionUID = 1L;
    @Autowired
    private StatFilter        statFilter;
    @Autowired
    private LogFilter         logFilter;
    @Value("${druid.stat.urlMappings}")
    private String statUrlMappings;
    
    @PostConstruct
    @Override
    public void init() throws SQLException {
        //wall,config
        super.getProxyFilters().add(statFilter);
        super.getProxyFilters().add(logFilter);
        super.init();
    }
  
    /**
     * druid页面监控filter
     * @author:  heyiwu 
     * @return
     */
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
    @ConfigurationProperties(prefix = "druid.web-stat.initParameters")
    public Map<String, String> druidWebStatInitParameters() {
        return new HashMap<String, String>();
    }
    /**
     * druid页面监控servlet
     * @author:  heyiwu 
     * @return
     */    
    @Bean
    public ServletRegistrationBean druidStatViewServlet() {
        ServletRegistrationBean srb = new ServletRegistrationBean(new StatViewServlet(), statUrlMappings);
        srb.setInitParameters(druidStatInitParameters());
        srb.setLoadOnStartup(1);
        return srb;
    }
    
    @Bean
    @ConfigurationProperties(prefix = "druid.stat.initParameters")
    public Map<String, String> druidStatInitParameters() {
        return new HashMap<String, String>();
    }
    
    @Component
    @ConfigurationProperties(prefix = "druid")
    class StatFilter extends com.alibaba.druid.filter.stat.StatFilter {
    }

    @Component
    @ConfigurationProperties(prefix = "druid")
    class LogFilter extends Slf4jLogFilter {
    }
}

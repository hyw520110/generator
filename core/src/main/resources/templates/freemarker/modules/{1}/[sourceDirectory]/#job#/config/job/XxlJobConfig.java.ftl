package ${packagePath};

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfig {
    @Value("${r"${xxl.job.admin.addresses:http://127.0.0.1:8080/xxl-job-admin}"}")
    private String adminAddresses;

    @Value("${r"${xxl.job.executor.appname:default-executor}"}")
    private String appname;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        return xxlJobSpringExecutor;
    }
}

package ${packagePath};

import ${global.rootPackage}.${global.projectName}.${moduleName}.interceptor.TenantWebInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 租户Web配置
 */
@Configuration
public class TenantWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantWebInterceptor()).addPathPatterns("/**");
    }
}

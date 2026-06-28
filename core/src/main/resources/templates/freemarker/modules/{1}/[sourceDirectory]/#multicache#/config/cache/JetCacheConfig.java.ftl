package ${packagePath};

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableMethodCache(basePackages = "${packagePath}.${projectName}")
@EnableCreateCacheAnnotation
public class JetCacheConfig {
    // 配合 application.yml 启用 Redis + Caffeine 两级缓存框架
}

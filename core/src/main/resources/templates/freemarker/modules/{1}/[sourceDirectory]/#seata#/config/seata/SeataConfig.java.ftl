package ${packagePath};

import org.springframework.context.annotation.Configuration;
// 引入 seata-spring-boot-starter 后，主要在 application.yml 中配置 tx-service-group
// 业务代码中使用 @io.seata.spring.annotation.GlobalTransactional 即可开启全局事务
@Configuration
public class SeataConfig {
}

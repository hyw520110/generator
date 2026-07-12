#if("plus"=="$mapperType")
package ${plusPackage};

import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
#if($global.features.contains('TENANT'))
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import ${global.rootPackage}.${global.projectName}.${moduleName}.config.tenant.TenantContextHolder;
import java.util.Arrays;
import java.util.List;
#end
#if($global.features.contains('DATAPERMISSION'))
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import ${global.rootPackage}.${global.projectName}.${moduleName}.config.permission.CustomDataPermissionHandler;
#end
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@Configuration
@MapperScan("${mapperPackage}")
public class MybatisPlusConfiguration {
	
    /**
     * MyBatis-Plus 插件配置（3.5.x 新版 API）
     * 包含分页插件和乐观锁插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        paginationInterceptor.setOverflow(false);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
#if($global.features.contains('TENANT'))
        // 多租户插件
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantId();
                if (tenantId == null) {
                    return new LongValue(1L);
                }
                return new LongValue(tenantId);
            }
            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }
            @Override
            public boolean ignoreTable(String tableName) {
                List<String> ignoreTables = Arrays.asList(
                    "sys_tenant", "sys_user", "sys_role", "sys_menu", "sys_dict", "sys_log", "sys_audit_log"
                );
                return ignoreTables.stream().anyMatch(t -> t.equalsIgnoreCase(tableName));
            }
        }));
#end
#if($global.features.contains('DATAPERMISSION'))
        // 数据权限插件
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new CustomDataPermissionHandler()));
#end
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 逻辑删除插件
     * @return
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new DefaultSqlInjector();
    }
}
#end
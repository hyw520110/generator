package ${packagePath};

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /**
     * 模块标题
     */
    String title() default "";

    /**
     * 业务类型（0其它 1新增 2修改 3删除）
     */
    int businessType() default 0;
}

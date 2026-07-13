package ${packagePath};

import java.lang.annotation.*;

/**
 * 切换数据源注解
 * 可以作用在类或方法上
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetDataSource {
    /**
     * 数据源名称，默认为主库
     */
    String value() default "master";
}

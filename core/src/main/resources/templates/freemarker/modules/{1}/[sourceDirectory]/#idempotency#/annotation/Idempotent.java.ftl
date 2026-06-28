package ${packagePath};

import java.lang.annotation.*;

/**
 * 接口幂等性注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /**
     * 幂等Token所在的 Header 或 参数名
     */
    String tokenName() default "Idempotent-Token";

    /**
     * 过期时间（秒）
     */
    long expireTime() default 60;
    
    /**
     * 错误提示信息
     */
    String message() default "请求太频繁，请稍后再试";
}

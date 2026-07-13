package ${packagePath};
import java.lang.annotation.*;
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String prefix() default "idemp:";
    long expire() default 5000L; // 锁过期时间5秒
}

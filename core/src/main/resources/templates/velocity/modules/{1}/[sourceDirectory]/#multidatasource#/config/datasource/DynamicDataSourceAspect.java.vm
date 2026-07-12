package ${packagePath};

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * 动态数据源切面
 * 优先级设置在事务之前（@Order(1)）保证在开启事务前先切换数据源
 */
@Aspect
@Order(1)
@Component
public class DynamicDataSourceAspect {

    @Pointcut("@annotation(${packagePath}.TargetDataSource) || @within(${packagePath}.TargetDataSource)")
    public void dataSourcePointCut() {
    }

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        TargetDataSource targetDataSource = method.getAnnotation(TargetDataSource.class);
        if (targetDataSource == null) {
            targetDataSource = point.getTarget().getClass().getAnnotation(TargetDataSource.class);
        }

        if (targetDataSource != null && StringUtils.hasText(targetDataSource.value())) {
            DynamicDataSourceContextHolder.setDataSource(targetDataSource.value());
        }

        try {
            return point.proceed();
        } finally {
            // 方法执行完必须清理，避免内存泄露以及连接池获取混乱
            DynamicDataSourceContextHolder.clearDataSource();
        }
    }
}

package ${packagePath};
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect {
    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint point, Log logAnno) throws Throwable {
        long beginTime = System.currentTimeMillis();
        // 记录日志：title = logAnno.title(), type = logAnno.type()
        Object result = point.proceed();
        long time = System.currentTimeMillis() - beginTime;
        // 此处可将 time, request参数, 操作人 存入 sys_log 表
        return result;
    }
}

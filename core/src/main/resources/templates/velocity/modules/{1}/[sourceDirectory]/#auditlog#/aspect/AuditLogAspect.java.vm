package ${packagePath};

import ${global.rootPackage}.${global.projectName}.${moduleName}.annotation.AuditLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 操作日志记录处理
 */
@Aspect
@Component
public class AuditLogAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    // 配置织入点
    @Pointcut("@annotation(${global.rootPackage}.${global.projectName}.${moduleName}.annotation.AuditLog)")
    public void logPointCut() {
    }

    /**
     * 处理完请求后执行
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            // TODO: 解析 Request, 获取 User，保存至数据库 sys_audit_log 表
            log.info("===> AuditLog Aspect Hit! Method: {}", joinPoint.getSignature().getName());
        } catch (Exception exp) {
            log.error("==异常通知异常==", exp);
        }
    }
}

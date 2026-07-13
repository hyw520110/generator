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
import ${servletPackage}.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String uri = request.getRequestURI();
                String httpMethod = request.getMethod();
                String ip = request.getRemoteAddr();
                
                // TODO: 获取当前登录用户 (例如从 Header 或 Session 中获取)
                String userId = request.getHeader("X-User-Id");
                if (userId == null) {
                    userId = "unknown";
                }

                // TODO: 将以下日志信息构建为实体并保存至 sys_audit_log 表
                log.info("===> AuditLog Aspect Hit! User: {}, IP: {}, URI: {}, HTTP Method: {}, Signature: {}", 
                        userId, ip, uri, httpMethod, joinPoint.getSignature().getName());
                
                if (e != null) {
                    log.error("===> AuditLog Exception: ", e);
                }
            }
        } catch (Exception exp) {
            log.error("==异常通知异常==", exp);
        }
    }
}

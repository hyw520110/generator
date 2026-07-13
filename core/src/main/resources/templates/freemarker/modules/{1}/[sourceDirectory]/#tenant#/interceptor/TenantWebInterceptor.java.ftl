package ${packagePath};

import ${global.rootPackage}.${global.projectName}.${moduleName}.config.tenant.TenantContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

<#if global.javaVersion == '8' || global.javaVersion == '11'>
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
<#else>
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
</#if>

/**
 * 租户拦截器
 */
public class TenantWebInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isEmpty()) {
            TenantContextHolder.setTenantId(Long.parseLong(tenantId));
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        TenantContextHolder.clear();
    }
}

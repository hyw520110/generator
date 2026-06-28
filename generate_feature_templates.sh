#!/bin/bash

# Create basic template files for Freemarker
FM_BASE="core/src/main/resources/templates/freemarker/modules/{1}/[sourceDirectory]"
VM_BASE="core/src/main/resources/templates/velocity/modules/{1}/[sourceDirectory]"

create_template() {
    local feature=$1
    local path=$2
    local filename=$3
    local content=$4
    
    # Freemarker
    mkdir -p "$FM_BASE/#${feature}#/${path}"
    echo "$content" > "$FM_BASE/#${feature}#/${path}/${filename}.ftl"
    
    # Velocity
    mkdir -p "$VM_BASE/#${feature}#/${path}"
    echo "$content" > "$VM_BASE/#${feature}#/${path}/${filename}.vm"
}

# AuditLog
create_template "auditlog" "aspect" "AuditLogAspect.java" "package \${basePackage}.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 自动操作日志切面
 */
@Aspect
@Component
public class AuditLogAspect {
    
    @Pointcut(\"@annotation(org.springframework.web.bind.annotation.PostMapping) || @annotation(org.springframework.web.bind.annotation.PutMapping) || @annotation(org.springframework.web.bind.annotation.DeleteMapping)\")
    public void auditPointcut() {
    }
    
    // TODO: 实现具体的环绕通知和日志记录逻辑
}
"

# Tenant
create_template "tenant" "config" "TenantConfiguration.java" "package \${basePackage}.config;

import org.springframework.context.annotation.Configuration;

/**
 * 多租户配置
 */
@Configuration
public class TenantConfiguration {
    // TODO: 注入 MybatisPlus 的 TenantLineInnerInterceptor
}
"

# Excel
create_template "excel" "utils" "ExcelUtils.java" "package \${basePackage}.utils;

/**
 * Excel 导入导出工具类
 */
public class ExcelUtils {
    // TODO: 封装 EasyExcel 或 POI 逻辑
}
"

# DataPermission
create_template "datapermission" "annotation" "DataPermission.java" "package \${basePackage}.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    String value() default \"\";
}
"

# XSS
create_template "xss" "filter" "XssFilter.java" "package \${basePackage}.filter;

import javax.servlet.*;
import java.io.IOException;

/**
 * XSS 过滤
 */
public class XssFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // TODO: 实现 XSS 请求包装器
        chain.doFilter(request, response);
    }
}
"

# Idempotency
create_template "idempotency" "annotation" "Idempotent.java" "package \${basePackage}.annotation;

import java.lang.annotation.*;

/**
 * 防重放/幂等注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    // 过期时间，默认5秒
    int timeout() default 5;
}
"

echo "Template generation complete."

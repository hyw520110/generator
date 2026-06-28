package org.hyw.tools.generator.enums;

import org.hyw.tools.generator.utils.StringUtils;

/**
 * 安全方案意图配置。
 * <p>
 * 生成器内部仍使用 Component / Feature 驱动模板渲染；该枚举用于避免用户手动组合出冲突配置。
 * </p>
 */
public enum SecurityScheme {
    NONE,
    SHIRO,
    SPRING_SECURITY,
    SPRING_SECURITY_OAUTH2;

    public boolean isShiro() {
        return this == SHIRO;
    }

    public boolean isSpringSecurity() {
        return this == SPRING_SECURITY || this == SPRING_SECURITY_OAUTH2;
    }

    public boolean isOauth2() {
        return this == SPRING_SECURITY_OAUTH2;
    }

    public static SecurityScheme from(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace('/', '_');
        if ("SPRINGSECURITY".equals(normalized) || "SPRING_SECURITY".equals(normalized)) {
            return SPRING_SECURITY;
        }
        if ("OAUTH2".equals(normalized) || "SPRINGSECURITY_OAUTH2".equals(normalized)
                || "SPRING_SECURITY_OAUTH2".equals(normalized)) {
            return SPRING_SECURITY_OAUTH2;
        }
        return SecurityScheme.valueOf(normalized);
    }
}

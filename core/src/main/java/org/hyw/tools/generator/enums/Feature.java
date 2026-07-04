package org.hyw.tools.generator.enums;

import org.hyw.tools.generator.utils.StringUtils;

/**
 * 功能/扩展特性定义
 * 用于区别于底层架构的 Component（技术组件），Feature 用于开启具体的业务或高级拓展能力。
 */
public enum Feature {
    // 多租户隔离
    TENANT,
    // 数据权限过滤
    DATAPERMISSION,
    I18N,
    DATAMASKING,
    DATAENCRYPTION,
    DEVOPS,
    CICD,
    UNITTEST,
    VALIDATION,
    AUDITLOG,
    IDEMPOTENCY,
    MULTIDATASOURCE,
    OSS,
    WEBSOCKET,
    EXCEL,
    SEATA,
    OAUTH2,
    MULTICACHE,
    XSS,
    WORKFLOW,
    JOB,
    // 通用工程架构约束与检查清单
    ARCHITECTURE_CONVENTIONS;

    private String alias;

    Feature() {
    }

    Feature(String alias) {
        this.alias = alias;
    }

    public String getAlias() {
        return StringUtils.isBlank(alias) ? this.name().toLowerCase() : alias;
    }

    public static Feature getFeature(String name) {
        if (name == null) {
            return null;
        }
        for (Feature f : values()) {
            if (f.name().equalsIgnoreCase(name) || (f.alias != null && f.alias.equalsIgnoreCase(name))) {
                return f;
            }
        }
        return null;
    }
}

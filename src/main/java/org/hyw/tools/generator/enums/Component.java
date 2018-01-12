package org.hyw.tools.generator.enums;

import org.hyw.tools.generator.utils.StringUtils;

public enum Component {
    MYBATIS,JPA, DUBBO,ZOOKEEPER,ROCKETMQ,SPRINGBOOT, SPRINGMVC,REDIS;
    public static Component getComonent(String name) {
        try {
            return valueOf(StringUtils.upperCase(name));
        } catch (Exception e) {
        }
        return null;
    }
}

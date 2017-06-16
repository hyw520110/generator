package com.tzg.tools.generator.enums;

import com.tzg.tools.generator.utils.StringUtils;

public enum Component {
    MYBATIS, DUBBO, SPRINGMVC;//, JPA, ;

    public static Component getComonent(String name) {
        try {
            return valueOf(StringUtils.upperCase(name));
        } catch (Exception e) {
        }
        return null;
    }
}

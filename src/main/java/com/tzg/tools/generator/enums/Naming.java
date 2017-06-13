package com.tzg.tools.generator.enums;

import com.tzg.tools.generator.utils.StringUtils;

public enum Naming {
    NOCHANGE("不做处理"), TOCAMEL("驼峰命名");

    private final String desc;

    Naming(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }


    

}

package org.hyw.tools.generator.enums;

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

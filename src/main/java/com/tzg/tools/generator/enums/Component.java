package com.tzg.tools.generator.enums;

import com.tzg.tools.generator.conf.ComponentConf;
import com.tzg.tools.generator.conf.dao.MyBatisConf;

public enum Component {
    MYBATIS(MyBatisConf.class), SPRINGMVC(MyBatisConf.class);//, JPA, ;
    private Class<? extends ComponentConf> confClass;

    private Component(Class<? extends ComponentConf> claz) {
        this.confClass = claz;
    }

    public Class<? extends ComponentConf> getConfClass() {
        return confClass;
    }

    public void setConfClass(Class<? extends ComponentConf> confClass) {
        this.confClass = confClass;
    }
}

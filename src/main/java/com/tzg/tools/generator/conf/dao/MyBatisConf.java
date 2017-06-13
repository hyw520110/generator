package com.tzg.tools.generator.conf.dao;

import com.tzg.tools.generator.conf.ComponentConf;
import com.tzg.tools.generator.dao.BaseDao;

/**
 * 
 * Filename:    MyBatisConf.java  
 * Description: 组件配置   
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     tzg.cn Inc.
 * @author:     heyiwu 
 * @version:    1.0  
 * Create at:   2017年6月12日 上午15:38:05  
 *
 */
public class MyBatisConf extends ComponentConf {
    /**
     * 是否在xml中添加二级缓存配置
     */
    private boolean enableCache  = true;
    /**
     * 开启 ActiveRecord 模式
     */
    private boolean activeRecord = true;

    /**
     * 开启 BaseResultMap
     */
    private boolean baseResultMap = false;

    /**
     * 开启 baseColumnList
     */
    private boolean baseColumnList = false;
    /**
     * dao类名
     */
    private String  daoName;
    /**
     * dao父类
     */
    private String superDaoClass = BaseDao.class.getName();
    /**
     * mapper xml名称
     */
    private String mapperXmlName;

    public boolean isEnableCache() {
        return enableCache;
    }

    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }

    public boolean isActiveRecord() {
        return activeRecord;
    }

    public void setActiveRecord(boolean activeRecord) {
        this.activeRecord = activeRecord;
    }

    public boolean isBaseResultMap() {
        return baseResultMap;
    }

    public void setBaseResultMap(boolean baseResultMap) {
        this.baseResultMap = baseResultMap;
    }

    public boolean isBaseColumnList() {
        return baseColumnList;
    }

    public void setBaseColumnList(boolean baseColumnList) {
        this.baseColumnList = baseColumnList;
    }

    public String getDaoName() {
        return daoName;
    }

    public void setDaoName(String daoName) {
        this.daoName = daoName;
    }

    public String getMapperXmlName() {
        return mapperXmlName;
    }

    public void setMapperXmlName(String mapperXmlName) {
        this.mapperXmlName = mapperXmlName;
    }

    public String getSuperDaoClass() {
        return superDaoClass;
    }

    public void setSuperDaoClass(String superDaoClass) {
        this.superDaoClass = superDaoClass;
    }

}

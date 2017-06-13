package com.tzg.tools.generator.conf;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tzg.tools.generator.dao.entity.BaseEntity;
import com.tzg.tools.generator.enums.Naming;
import com.tzg.tools.generator.service.BaseService;
import com.tzg.tools.generator.utils.StringUtils;

/**
 * 
 * Filename:    StrategyConf.java  
 * Description: 策略配置   
 * Copyright:   Copyright (c) 2015-2018 All Rights Reserved.
 * Company:     tzg.cn Inc.
 * @author:     heyiwu 
 * @version:    1.0  
 * Create at:   2017年6月13日 上午10:01:08  
 *
 */
public class StrategyConf {
    /**
     * 表名、字段名、是否使用下划线命名（默认 false）
     */
    public boolean columnUnderline = false;

    /**
     * 是否大写命名
     */
    private boolean isCapitalMode = false;

    /**
     * 数据库表映射到实体的命名策略
     */
    private Naming naming = Naming.NOCHANGE;

    /**
     * 表前缀
     */
    private String[] tablePrefix;

    /**
     * 自定义继承的Entity类全称，带包名
     */
    private String superEntityClass = BaseEntity.class.getName();

    /**
     * 自定义基础的Entity类，公共字段
     */
    private String[] superEntityColumns;

    /**
     * 自定义继承的Service类全称，带包名
     */
    private String superServiceClass = BaseService.class.getName();

    /**
     * 自定义继承的Controller类全称，带包名
     */
    private String superControllerClass;

    /**
     * 需要包含的表名（与exclude二选一配置）
     */
    private String[] include = null;

    /**
     * 需要排除的表名
     */
    private String[] exclude        = null;
    /**
     *是否生成实体字段常量（默认 false） 
     */
    private boolean  columnConstant = false;

    /**
     * 【实体】是否为构建者模型（默认 false） 
     */
    private boolean builderModel = false;

    /**
     * 父包名,如果为空，将下面子包名必须写全部， 否则就只需写子包名
     */
    private String rootPackage = "com.tzg";

    /**
     * 模块名。
     */
    private String moduleName = null;

    /**
     * Entity包名
     */
    private String entity = "entity";

    /**
     * Service包名
     */
    private String service = "service";

    /**
     * Service Impl包名
     */
    private String serviceImpl = "service.impl";
    /**
     * Mapper包名
     */
    private String dao         = "dao";

    /**
     * Controller包名
     */
    private String controller = "web";

    public boolean isColumnUnderline() {
        return columnUnderline;
    }

    public void setColumnUnderline(boolean columnUnderline) {
        this.columnUnderline = columnUnderline;
    }

    public boolean isCapitalMode() {
        return isCapitalMode;
    }

    public void setCapitalMode(boolean isCapitalMode) {
        this.isCapitalMode = isCapitalMode;
    }

    public Naming getNaming() {
        return naming;
    }

    public void setNaming(Naming naming) {
        this.naming = naming;
    }

    public String[] getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String[] tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public String getSuperEntityClass() {
        return superEntityClass;
    }

    public void setSuperEntityClass(String superEntityClass) {
        this.superEntityClass = superEntityClass;
    }

    public String[] getSuperEntityColumns() {
        return superEntityColumns;
    }

    public void setSuperEntityColumns(String[] superEntityColumns) {
        this.superEntityColumns = superEntityColumns;
    }

    public String getSuperServiceClass() {
        return superServiceClass;
    }

    public void setSuperServiceClass(String superServiceClass) {
        this.superServiceClass = superServiceClass;
    }

    public String getSuperControllerClass() {
        return superControllerClass;
    }

    public void setSuperControllerClass(String superControllerClass) {
        this.superControllerClass = superControllerClass;
    }

    public String[] getInclude() {
        return include;
    }

    public void setInclude(String[] include) {
        this.include = include;
    }

    public String[] getExclude() {
        return exclude;
    }

    public void setExclude(String[] exclude) {
        this.exclude = exclude;
    }

    public boolean isColumnConstant() {
        return columnConstant;
    }

    public void setColumnConstant(boolean columnConstant) {
        this.columnConstant = columnConstant;
    }

    public boolean isBuilderModel() {
        return builderModel;
    }

    public void setBuilderModel(boolean builderModel) {
        this.builderModel = builderModel;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getServiceImpl() {
        return serviceImpl;
    }

    public void setServiceImpl(String serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    public String getDao() {
        return dao;
    }

    public void setDao(String dao) {
        this.dao = dao;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public String getRootPackage() {
        return rootPackage;
    }

    public void setRootPackage(String rootPackage) {
        this.rootPackage = rootPackage;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    //TODO 
    public boolean isCapitalModeNaming(String name) {
        return isCapitalMode && StringUtils.isCapitalMode(name);
    }
}

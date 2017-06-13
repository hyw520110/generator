package com.tzg.tools.generator.conf.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tzg.tools.generator.annotations.TabId;
import com.tzg.tools.generator.conf.StrategyConf;
import com.tzg.tools.generator.enums.IdType;
import com.tzg.tools.generator.utils.StringUtils;

public class Table {


    private boolean convert;
    private String name;
    private String comment;

    private String entityName;
    private String mapperName;
    private String xmlName;
    private String serviceName;
    private String serviceImplName;
    private String controllerName;

    private List<TabField> fields;
    private List<TabField> commonFields;// 公共字段
    private List<String> importPackages = new ArrayList<>();
    private String fieldNames;
    public Table(String name) {
        this.name=name;
    }
    public boolean isConvert() {
        return convert;
    }

    protected void setConvert(StrategyConf conf) {
        if (StringUtils.startWithTablePrefix(name,conf.getTablePrefix())) {
            // 包含前缀
            this.convert = true;
        } else if (conf.isCapitalModeNaming(name)) {
            // 包含
            this.convert = false;
        } else {
            // 转换字段
            if (conf.columnUnderline) {
                // 包含大写处理
                if (StringUtils.containsUpperCase(name)) {
                    this.convert = true;
                }
            } else if (!entityName.equalsIgnoreCase(name)) {
                this.convert = true;
            }
        }
    }

    public void setConvert(boolean convert) {
        this.convert = convert;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getEntityPath() {
        StringBuilder ep = new StringBuilder();
        ep.append(entityName.substring(0, 1).toLowerCase());
        ep.append(entityName.substring(1));
        return ep.toString();
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(StrategyConf strategyConf, String entityName) {
        this.entityName = entityName;
        this.setConvert(strategyConf);
    }

    public String getMapperName() {
        return mapperName;
    }

    public void setMapperName(String mapperName) {
        this.mapperName = mapperName;
    }

    public String getXmlName() {
        return xmlName;
    }

    public void setXmlName(String xmlName) {
        this.xmlName = xmlName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceImplName() {
        return serviceImplName;
    }

    public void setServiceImplName(String serviceImplName) {
        this.serviceImplName = serviceImplName;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public List<TabField> getFields() {
        return fields;
    }

    public void setFields(List<TabField> fields) {
        if (CollectionUtils.isNotEmpty(fields)) {
            this.fields = fields;
            // 收集导入包信息
            Set<String> list = new HashSet<>();
            for (TabField field : fields) {
                if (null != field.getColumnType()) {
                    list.add(field.getColumnType().getClaz()==null?field.getColumnType().getType():field.getColumnType().getClaz().getName());
                }
                if (field.isKeyFlag()) {
                    // 主键
                    if (field.isConvert() || field.isKeyIdentityFlag()) {
                        list.add(TabId.class.getName());
                    }
                    // 自增
                    if (field.isKeyIdentityFlag()) {
                        list.add(IdType.class.getName());
                    }
                } else if (field.isConvert()) {
                    // 普通字段
                    list.add(TabField.class.getName());
                }
            }
            if (list.isEmpty()) {
                return ;
            }
            this.importPackages = new ArrayList<>(Arrays.asList(list.toArray(new String[]{})));
        }
    }

    public List<TabField> getCommonFields() {
        return commonFields;
    }

    public void setCommonFields(List<TabField> commonFields) {
        this.commonFields = commonFields;
    }

    public List<String> getImportPackages() {
        return importPackages;
    }

    public void addImportPackages(String pkg) {
        importPackages.add(pkg);
    }

    /**
     * 转换filed实体为xmlmapper中的basecolumn字符串信息
     *
     * @return
     */
    public String getFieldNames() {
        if (StringUtils.isEmpty(fieldNames)) {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < fields.size(); i++) {
                TabField fd = fields.get(i);
                if (i == fields.size() - 1) {
                    names.append(cov2col(fd));
                } else {
                    names.append(cov2col(fd)).append(", ");
                }
            }
            fieldNames = names.toString();
        }
        return fieldNames;
    }

    /**
     * mapper xml中的字字段添加as
     *
     * @param field 字段实体
     * @return 转换后的信息
     */
    private String cov2col(TabField field) {
        if (null != field) {
            return field.isConvert() ? field.getName() + " AS " + field.getPropertyName() : field.getName();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
}

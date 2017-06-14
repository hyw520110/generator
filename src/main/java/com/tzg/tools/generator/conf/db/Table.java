package com.tzg.tools.generator.conf.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.tzg.tools.generator.conf.BaseBean;
import com.tzg.tools.generator.utils.StringUtils;

public class Table extends BaseBean {

    private static final long serialVersionUID = 1L;
    /**
     * 表名 
     */
    private String            name;
    /**
     * 表说明
     */
    private String            comment;

    private String entityName;

    private List<TabField> fields;
    private List<TabField> commonFields;                      // 公共字段
    private List<String>   importPackages = new ArrayList<>();
    private String         fieldNames;

    public Table(String name) {
        this.name = name;
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

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public List<TabField> getFields() {
        return fields;
    }

    public void setFields(List<TabField> fields) {
        this.fields = fields;
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        // 收集导入包信息
        Set<String> list = new HashSet<>();
        for (TabField field : fields) {
            if (null != field.getColumnType() && field.getColumnType().getClaz() != null) {
                list.add(field.getColumnType().getClaz().getName());
            }
        }
        if (list.isEmpty()) {
            return;
        }
        this.importPackages = new ArrayList<>(Arrays.asList(list.toArray(new String[] {})));

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
        if (StringUtils.isNotEmpty(fieldNames)) {
            return fieldNames;
        }
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            TabField f = fields.get(i);
            if (i == fields.size() - 1) {
                names.append(getColumnName(f));
            } else {
                names.append(getColumnName(f)).append(", ");
            }
        }
        fieldNames = names.toString();
        return fieldNames;
    }

    /**
     * mapper xml中的字字段添加as
     * @param field 字段实体
     * @return 转换后的信息
     */
    private String getColumnName(TabField field) {
        if (null != field) {
            return field.isNameChange() ? field.getName() + " AS " + field.getPropertyName() : field.getName();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}

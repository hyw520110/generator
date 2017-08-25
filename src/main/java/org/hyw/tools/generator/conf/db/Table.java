package org.hyw.tools.generator.conf.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hyw.tools.generator.conf.BaseBean;
import org.hyw.tools.generator.utils.StringUtils;

public class Table extends BaseBean {

	private static final long serialVersionUID = 1L;
	/**
	 * 表名
	 */
	private String name;
	/**
	 * 表说明
	 */
	private String comment;

	/**
	 * bean名称
	 */
	private String beanName;
	/**
	 * 所有字段
	 */
	private List<TabField> fields;

	/**
	 * 导包
	 */
	private List<String> importPackages = new ArrayList<>();
	/**
	 * 所有的字段名和属性名的对应字符串 columnName as fieldName,...
	 */
	private String fieldNames;

	/**
	 * 添加表字段
	 */
	public void addField(TabField tabField) {
		if (containField(tabField)) {
			return;
		}
		getFields().add(tabField);
		if (null == tabField.getFieldType() || tabField.getFieldType().getClaz() == null) {
			return;
		}
		addImportPackages(tabField.getFieldType().getClaz().getName());
	}

	/**
	 * 判断字段是否存在,用于判断字段名处理后(去除指定前缀),是否存在同名字段。 如：表存在id和batch_id字段，配置移除batch_前缀
	 * 
	 * @param tabField
	 * @return
	 */
	public boolean containField(TabField tabField) {
		return getFields().contains(tabField);
	}

	/**
	 * 获取第一个主键
	 * 
	 * @return
	 */
	public TabField getPrimaryKeyField() {
		List<TabField> list = getPrimarykeyFields();
		if (null != list && !list.isEmpty()) {
			return list.get(0);
		}
		return null;
	}

	/**
	 * 获取主键字段
	 * 
	 * @author: heyiwu
	 * @return
	 */
	public List<TabField> getPrimarykeyFields() {
		ArrayList<TabField> list = new ArrayList<>();
		List<TabField> fields = getFields();
		for (TabField tabField : fields) {
			if (tabField.isPrimarykey()) {
				list.add(tabField);
			}
		}
		return list;
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
	 * 
	 * @param field
	 *            字段实体
	 * @return 转换后的信息
	 */
	private String getColumnName(TabField field) {
		if (null != field) {
			return field.isNameChange() ? field.getName() + " AS " + field.getPropertyName() : field.getName();
		}
		return StringUtils.EMPTY;
	}

	public Table(String name, String comment) {
		this.name = name;
		this.comment = comment;
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

	public String getBeanName() {
		return beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public List<TabField> getFields() {
		if (null == fields) {
			fields = new ArrayList<>();
		}
		return fields;
	}

	public List<String> getImportPackages() {
		return importPackages;
	}

	public void addImportPackages(String pkg) {
		importPackages.add(pkg);
	}

	public void setFields(List<TabField> fields) {
		this.fields = fields;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}

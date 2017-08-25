package org.hyw.tools.generator.conf.db;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hyw.tools.generator.conf.BaseBean;
import org.hyw.tools.generator.enums.FieldType;

public class TabField extends BaseBean {
	private static final long serialVersionUID = 1L;

	/**
	 * 字段名
	 */
	private String name;
	/**
	 * 字段类型
	 */
	private String type;
	/**
	 * 字段说明
	 */
	private String comment;
	/**
	 * java属性名
	 */
	private String propertyName;
	/**
	 * 属性类型
	 */
	private FieldType fieldType;

	/**
	 * TODO
	 */
	private String jdbcType;

	// TODO
	private boolean nullAble;
	/**
	 * 是否主键
	 */
	private boolean isPrimarykey;
	/**
	 * 主键是否为自增类型
	 */
	private boolean isIdentity;

	/**
	 * 是否公共字段
	 */
	private boolean isCommonField;

	public TabField(String name, String type) {
		this.name = name;
		this.type = type;
	}

	public boolean isPrimarykey() {
		return isPrimarykey;
	}

	public void setPrimarykey(boolean isPrimarykey) {
		this.isPrimarykey = isPrimarykey;
	}

	public boolean isIdentity() {
		return isIdentity;
	}

	public void setIdentity(boolean isIdentity) {
		this.isIdentity = isIdentity;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getPropertyType() {
		if (null != fieldType) {
			return fieldType.getType();
		}
		return null;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public boolean isNullAble() {
		return nullAble;
	}

	public void setNullAble(boolean nullAble) {
		this.nullAble = nullAble;
	}

	/**
	 * 按JavaBean规则来生成get和set方法
	 */
	public String getCapitalName() {
		if (propertyName.length() <= 1) {
			return propertyName.toUpperCase();
		}
		// 第一个字母 小写、 第二个字母 大写 ，特殊处理
		String firstChar = propertyName.substring(0, 1);
		if (Character.isLowerCase(firstChar.toCharArray()[0])
				&& Character.isUpperCase(propertyName.substring(1, 2).toCharArray()[0])) {
			return firstChar.toLowerCase() + propertyName.substring(1);
		}
		return firstChar.toUpperCase() + propertyName.substring(1);
	}

	public boolean isNameChange() {
		return !getName().equals(getPropertyName());
	}

	public FieldType getFieldType() {
		return fieldType;
	}

	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	public String getJdbcType() {
		return this.jdbcType;
	}

	public void setJdbcType(String jdbcType) {
		this.jdbcType = jdbcType;
	}

	public boolean isCommonField() {
		return isCommonField;
	}

	public void setCommonField(boolean isCommonField) {
		this.isCommonField = isCommonField;
	}

	@Override
	public boolean equals(Object arg) {
		if (null == arg) {
			return null == this;
		}
		if (!(arg instanceof TabField)) {
			return false;
		}
		//同一张表里不可能存在同名的字段
		return new EqualsBuilder().append(this.getName(), null == arg ? null : ((TabField) arg).getName()).isEquals();
	}

}

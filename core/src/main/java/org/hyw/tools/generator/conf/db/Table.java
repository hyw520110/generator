package org.hyw.tools.generator.conf.db;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hyw.tools.generator.conf.BaseBean;
import org.hyw.tools.generator.enums.FieldType;
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
	 * 表创建时间
	 */
	private String createTime;
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
	
	public String getPrimaryKeyClass() {
		List<TabField> list = getPrimarykeyFields();
		if (null != list && list.size()>=1) {
			return list.get(0).getFieldType().getType();
		}
		//TODO 复合主键处理
		return null;
	}
	/**
	 * 是否有主键
	 * TODO 无主键
	 * @return
	 */
	public boolean hasPrimarykeys() {
		return !getPrimarykeyFields().isEmpty();
	}

	/**
	 * 获取主键字段
	 * 
	 * @author: heyiwu
	 * @return
	 */
	public List<TabField> getPrimarykeyFields() {
		List<TabField> list = new LinkedList<>();
		List<TabField> fields = getFields();
		for (TabField tabField : fields) {
			if (tabField.isPrimarykey()) {
				list.add(tabField);
			}
		}
		return list;
	}

	public String getPrimarykeyFieldsNames() {
		if (!hasPrimarykeys()) {
			return "";
		}
		StringBuilder builder=new StringBuilder();
		List<TabField> list = getPrimarykeyFields();
		for (TabField tabField : list) {
			builder.append(tabField.getName()+",");
		}
		return builder.deleteCharAt(builder.length()-1).toString();
	}

	/**
	 * @return
	 */
	public String getFieldNames() {
		if (StringUtils.isNotEmpty(fieldNames)) {
			return fieldNames;
		}
		StringBuilder names = new StringBuilder();
		for (TabField f : fields) {
			names.append(f.getName()).append(",");
		}
		fieldNames = names.deleteCharAt(names.length() - 1).toString();
		return fieldNames;
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
	public String getLowercaseBeanName() {
		return StringUtils.lowercaseFirst(getBeanName());
	}
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public List<TabField> getFields() {
		if (null == fields) {
			fields = new LinkedList<>();
		}
		return fields;
	}

	public int getFieldsSize() {
		return getFields().size();
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

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}
}

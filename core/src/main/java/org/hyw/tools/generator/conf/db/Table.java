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
		if (null == list || list.isEmpty()) {
			return "Long"; // 默认主键类型
		}
		if (list.size() == 1) {
			// 单主键：返回主键字段类型
			return list.get(0).getFieldType().getType();
		}
		// 复合主键：返回复合主键类名
		return getBeanName() + "Key";
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
			// 使用 propertyName 而不是 name，确保使用 Java 属性名（首字母小写）
			String propName = tabField.getPropertyName();
			// 如果 propertyName 首字母是大写，转为小写
			if (propName.length() > 0 && Character.isUpperCase(propName.charAt(0))) {
				propName = Character.toLowerCase(propName.charAt(0)) + propName.substring(1);
			}
			builder.append(propName).append(",");
		}
		return builder.length() > 0 ? builder.deleteCharAt(builder.length()-1).toString() : "";
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

	/**
	 * 获取字段列表
	 */
	public List<TabField> getFields() {
		if (null == fields) {
			fields = new LinkedList<>();
		}
		return fields;
	}
	
	/**
	 * 获取排序后的字段列表（按重要性排序：主键 -> 非空字段 -> 空字段）
	 * @return 排序后的字段列表
	 */
	public List<TabField> getSortedFields() {
		if (null == fields || fields.isEmpty()) {
			return fields;
		}
		
		List<TabField> sortedFields = new LinkedList<>(fields);
		sortedFields.sort((f1, f2) -> {
			// 主键字段优先
			if (f1.isPrimarykey() && !f2.isPrimarykey()) {
				return -1;
			}
			if (!f1.isPrimarykey() && f2.isPrimarykey()) {
				return 1;
			}
			
			// 都不是主键，按是否为空排序
			if (!f1.isNullAble() && f2.isNullAble()) {
				return -1;
			}
			if (f1.isNullAble() && !f2.isNullAble()) {
				return 1;
			}
			
			// 都是非空或都是可空，按字段名排序保证稳定性
			return f1.getName().compareTo(f2.getName());
		});
		
		return sortedFields;
	}
	
	/**
	 * 获取原始字段列表（未排序）
	 */
	public List<TabField> getRawFields() {
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
	
	/**
	 * 判断是否为复合主键
	 */
	public boolean isCompositePrimaryKey() {
		return getPrimarykeyFields().size() > 1;
	}
	
	/**
	 * 获取主键信息
	 */
	public PrimaryKeyInfo getPrimaryKeyInfo() {
		if (!hasPrimarykeys()) {
			return null;
		}
		List<TabField> pkFields = getPrimarykeyFields();
		// 传入空的包名作为默认值，实际使用时可以通过其他方式设置
		return new PrimaryKeyInfo(pkFields, "");
	}
	
	/**
	 * 获取主键类类型
	 */
	public Class<?> getPrimaryKeyClassType() {
		List<TabField> pkFields = getPrimarykeyFields();
		if (pkFields.isEmpty()) {
			return Long.class;
		}
		if (pkFields.size() == 1) {
			return pkFields.get(0).getFieldType().getClaz();
		}
		return Object.class;
	}
	
	/**
	 * 计算字段的推荐列宽（百分比格式）
	 * @param field 字段
	 * @param fieldCount 总字段数
	 * @return 推荐的列宽百分比（字符串格式，如 "12%"）
	 */
	public String calculateFieldWidthPercent(TabField field, int fieldCount) {
		if (field == null || fieldCount <= 0) {
			return "10%";
		}
		
		// 字段数过多时，使用更紧凑的分配
		int fieldCountAdjust = Math.min(fieldCount, 15);
		
		// 基础百分比（平均分配）
		double basePercent = 100.0 / fieldCountAdjust;
		
		// 根据字段重要性调整权重
		double importanceWeight = 1.0;
		if (field.isPrimarykey()) {
			importanceWeight = 1.5; // 主键字段权重高
		} else if (!field.isNullAble()) {
			importanceWeight = 1.2; // 非空字段权重中等
		} else {
			importanceWeight = 0.8; // 可空字段权重低
		}
		
		// 根据字段类型调整权重
		String type = field.getType() != null ? field.getType().toLowerCase() : "";
		if (type.contains("text") || type.contains("blob")) {
			importanceWeight *= 1.3; // 大文本字段需要更多空间
		} else if (type.contains("date") || type.contains("time")) {
			importanceWeight *= 1.2; // 日期字段需要更多空间
		} else if (type.contains("int") || type.contains("tinyint") || type.contains("smallint")) {
			importanceWeight *= 0.7; // 数字类型需要较少空间
		}
		
		// 根据字段名称调整权重
		String name = field.getName() != null ? field.getName().toLowerCase() : "";
		if (name.contains("name") || name.contains("title") || name.contains("description")) {
			importanceWeight *= 1.3; // 名称/标题字段需要更多空间
		} else if (name.contains("id") || name.contains("code") || name.contains("no") || name.contains("type") || name.contains("status")) {
			importanceWeight *= 0.7; // 编码/状态字段需要较少空间
		}
		
		// 计算最终百分比
		double finalPercent = basePercent * importanceWeight;
		
		// 限制范围（最小5%，最大30%）
		finalPercent = Math.max(5.0, Math.min(30.0, finalPercent));
		
		// 格式化为整数百分比
		return (int) Math.round(finalPercent) + "%";
	}
	
	/**
	 * 获取字段的列宽配置字符串（百分比格式）
	 * @param field 字段
	 * @return 列宽配置字符串，如 "width: '12%'" 或 ""（表示不设置宽度）
	 */
	public String getFieldWidthConfig(TabField field) {
		int fieldCount = getFieldsSize();
		String width = calculateFieldWidthPercent(field, fieldCount);
		return "width: '" + width + "'";
	}
	
	/**
	 * 判断字段是否需要固定列
	 * @param field 字段
	 * @param index 字段在排序后列表中的索引
	 * @return "fixed: 'left'" 或 "fixed: 'right'" 或 ""
	 */
	public String getFieldFixedConfig(TabField field, int index) {
		// 序号列左固定（序号列不在此方法中处理，在模板中单独配置）
		if ("serial".equals(field.getPropertyName())) {
			return "fixed: 'left'";
		}
		
		// 操作列右固定（操作列不在此方法中处理，在模板中单独配置）
		if ("action".equals(field.getPropertyName())) {
			return "fixed: 'right'";
		}
		
		// 主键字段左固定（字段数较多时）
		int fieldCount = getFieldsSize();
		if (field.isPrimarykey() && fieldCount > 10) {
			return "fixed: 'left'";
		}
		
		// 前几个重要的非空字段也可以左固定（字段数很多时）
		if (fieldCount > 15 && index < 3 && !field.isNullAble()) {
			return "fixed: 'left'";
		}
		
		return "";
	}
}

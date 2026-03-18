package org.hyw.tools.generator.conf.db;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import org.hyw.tools.generator.utils.StringUtils;

/**
 * 复合主键信息封装
 * 
 * @author heyiwu
 * @version 2.0
 */
@Getter
public class PrimaryKeyInfo {
    
    /**
     * 是否复合主键
     */
    private final boolean composite;
    
    /**
     * 主键字段列表
     */
    private final List<TabField> fields;
    
    /**
     * 主键类名（复合主键时使用）
     */
    private final String keyClassName;
    
    /**
     * 主键类包名（复合主键时使用）
     */
    private final String keyClassPackage;
    
    public PrimaryKeyInfo(List<TabField> fields, String entityPackage) {
        this.fields = fields;
        this.composite = fields.size() > 1;
        
        if (composite && !fields.isEmpty()) {
            // 从第一个主键字段所在表推导实体名
            // 表名转实体名：t_order_item -> OrderItem
            String tableName = fields.get(0).getName();
            String entityName = StringUtils.removePrefixAndCamel(tableName, new String[]{"t_", "tb_"}, new char[]{'_'});
            this.keyClassName = entityName + "Key";
            this.keyClassPackage = entityPackage + ".key";
        } else {
            this.keyClassName = null;
            this.keyClassPackage = null;
        }
    }
    
    /**
     * 获取主键字段数量
     */
    public int size() {
        return fields.size();
    }
    
    /**
     * 获取第一个主键字段（单主键时使用）
     */
    public TabField getFirstField() {
        return fields.isEmpty() ? null : fields.get(0);
    }
    
    /**
     * 获取主键类型（单主键时返回字段类型，复合主键时返回主键类名）
     */
    public String getKeyType() {
        if (composite) {
            return keyClassName;
        }
        return getFirstField().getPropertyType();
    }
    
    /**
     * 获取主键字段名（逗号分隔，用于 SQL）
     */
    public String getFieldNames() {
        return fields.stream()
            .map(TabField::getName)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * 获取主键属性名（逗号分隔，用于 Java 代码）
     */
    public String getPropertyNames() {
        return fields.stream()
            .map(TabField::getPropertyName)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * 获取主键字段（用于 Velocity 模板遍历）
     */
    public List<TabField> getFields() {
        return fields;
    }
    
    /**
     * 生成主键参数列表（用于方法参数）
     * 示例：Long orderId, Long itemId, Integer batchNo
     */
    public String getParameterList() {
        return fields.stream()
            .map(f -> f.getPropertyType() + " " + f.getPropertyName())
            .collect(Collectors.joining(", "));
    }
    
    /**
     * 生成主键参数注解列表（用于 Spring Controller）
     * 示例：@PathVariable("orderId") Long orderId, @PathVariable("itemId") Long itemId
     */
    public String getAnnotatedParameterList(String annotationType) {
        return fields.stream()
            .map(f -> {
                String annotation = getAnnotation(annotationType, f.getPropertyName());
                return annotation + " " + f.getPropertyType() + " " + f.getPropertyName();
            })
            .collect(Collectors.joining(", "));
    }
    
    /**
     * 获取注解字符串
     */
    private String getAnnotation(String type, String paramName) {
        switch (type) {
            case "PathVariable":
                return "@PathVariable(\"" + paramName + "\")";
            case "RequestParam":
                return "@RequestParam(\"" + paramName + "\")";
            case "Param":
                return "@Param(\"" + paramName + "\")";
            default:
                return "";
        }
    }
    
    /**
     * 生成主键值列表（用于方法调用）
     * 示例：orderId, itemId, batchNo
     */
    public String getValueList() {
        return fields.stream()
            .map(TabField::getPropertyName)
            .collect(Collectors.joining(", "));
    }
    
    /**
     * 生成主键设置代码（用于 DTO 转 Entity）
     * 示例：
     * entity.setOrderId(dto.getOrderId());
     * entity.setItemId(dto.getItemId());
     */
    public String generateSetterCode(String targetVar, String sourceVar) {
        StringBuilder sb = new StringBuilder();
        for (TabField field : fields) {
            String setter = "set" + field.getCapitalName();
            String getter = "get" + field.getCapitalName();
            sb.append(targetVar).append(".").append(setter)
              .append("(").append(sourceVar).append(".").append(getter)
              .append(");\n");
        }
        return sb.toString();
    }
    
    /**
     * 生成复合主键类代码
     */
    public String generateKeyClassCode(String entityPackage, String author) {
        if (!composite) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String packageName = getKeyClassPackage();
        String className = getKeyClassName();
        
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import java.io.Serializable;\n");
        sb.append("import java.util.Objects;\n\n");
        sb.append("import lombok.Data;\n");
        sb.append("import lombok.NoArgsConstructor;\n");
        sb.append("import lombok.AllArgsConstructor;\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(entityPackage.substring(entityPackage.lastIndexOf('.') + 1))
          .append(" 复合主键类\n");
        sb.append(" * \n");
        sb.append(" * @author ").append(author).append("\n");
        sb.append(" */\n");
        sb.append("@Data\n");
        sb.append("@NoArgsConstructor\n");
        sb.append("@AllArgsConstructor\n");
        sb.append("public class ").append(className).append(" implements Serializable {\n\n");
        sb.append("    private static final long serialVersionUID = 1L;\n\n");
        
        for (TabField field : fields) {
            sb.append("    /** ").append(field.getComment()).append(" */\n");
            sb.append("    private ").append(field.getPropertyType())
              .append(" ").append(field.getPropertyName()).append(";\n\n");
        }
        
        sb.append("    @Override\n");
        sb.append("    public boolean equals(Object o) {\n");
        sb.append("        if (this == o) return true;\n");
        sb.append("        if (o == null || getClass() != o.getClass()) return false;\n");
        sb.append("        ").append(className).append(" that = (").append(className).append(") o;\n");
        sb.append("        return Objects.equals(").append(fields.get(0).getPropertyName())
          .append(", that.").append(fields.get(0).getPropertyName()).append(")");
        
        for (int i = 1; i < fields.size(); i++) {
            sb.append("\n            && Objects.equals(").append(fields.get(i).getPropertyName())
              .append(", that.").append(fields.get(i).getPropertyName()).append(")");
        }
        sb.append(";\n");
        sb.append("    }\n\n");
        
        sb.append("    @Override\n");
        sb.append("    public int hashCode() {\n");
        sb.append("        return Objects.hash(").append(getValueList()).append(");\n");
        sb.append("    }\n");
        sb.append("}\n");
        
        return sb.toString();
    }
}

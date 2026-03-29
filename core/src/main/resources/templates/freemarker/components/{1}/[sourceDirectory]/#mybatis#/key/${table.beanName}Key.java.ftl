<#--  复合主键类模板 -->
<#--  使用场景：当表有 2 个或以上主键字段时生成此类 -->
<#--  如果不是复合主键，输出空内容，文件将被跳过 -->
<#if !table?? || !table.isCompositePrimaryKey()>
</#if>
<#if table?? && table.isCompositePrimaryKey()>
package ${rootPackage}.key;

import java.io.Serializable;
import java.util.Objects;

<#if lombok?? && lombok>
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
</#if>

/**
 * ${table.beanName}复合主键类
 * 主键字段：${table.primaryKeyInfo.fieldNames}
 *
 * @author ${author}
 */
<#if lombok?? && lombok>
@Data
@NoArgsConstructor
@AllArgsConstructor
</#if>
public class ${table.beanName}Key implements Serializable {

    private static final long serialVersionUID = 1L;

    <#list table.primaryKeyInfo.fields as field>
    /** ${field.comment} */
    private ${field.fieldType.type} ${field.propertyName};
    </#list>

<#if !lombok?? || !lombok>
    <#list table.primaryKeyInfo.fields as field>
    public ${field.fieldType.type} get${field.capitalName}() {
        return ${field.propertyName};
    }

    public void set${field.capitalName}(${field.fieldType.type} ${field.propertyName}) {
        this.${field.propertyName} = ${field.propertyName};
    }

    </#list>

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ${table.beanName}Key that = (${table.beanName}Key) o;
        return <#list table.primaryKeyInfo.fields as field>Objects.equals(${field.propertyName}, that.${field.propertyName})<#if field?has_next> && </#if></#list>;
    }

    @Override
    public int hashCode() {
        return Objects.hash(<#list table.primaryKeyInfo.fields as field>${field.propertyName}<#if field?has_next>, </#if></#list>);
    }

    @Override
    public String toString() {
        return "${table.beanName}Key{" +
        <#list table.primaryKeyInfo.fields as field>"${field.propertyName}=" + ${field.propertyName}<#if field?has_next> + ", " + </#if></#list> +
        '}';
    }
</#if>
}
</#if>

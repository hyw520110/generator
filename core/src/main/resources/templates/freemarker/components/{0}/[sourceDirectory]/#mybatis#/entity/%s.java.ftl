package ${entityPackage!};


<#list table.importPackages as pkg>
import ${pkg!};
</#list>
<#if superEntityClass?? && !superEntityClass?contains('.')>
import ${entityPackage!}.${superEntityClass!};
<#else>
import java.io.Serializable;
</#if>
<#if sqlType?? && sqlType == "plus">
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
</#if>
import ${validationPackage}.constraints.NotBlank;
import ${validationPackage}.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

<#include 'comments/comment.ftl'>
<#if table.comment??>
@Schema(name = "${(className!'')?j_string}", description = "${(table.comment!'')?j_string}")
</#if>
<#if sqlType?? && sqlType == "plus">
@TableName("${table.name!}")
</#if>
public class ${className!} <#if superEntityClass??> extends ${superEntityClass!}<#if sqlType?? && sqlType == "plus"><${className!}></#if><#else> implements Serializable </#if>{

    private static final long serialVersionUID = 1L;

<#list table.fields as field>

<#if field.comment?has_content>

    /**

    * ${field.comment!}

    */

</#if>



<#if sqlType?? && sqlType == "plus">
<#if field.primarykey>
<#if table.primaryKeyCount == 1>
	@TableId(value = "${field.name!}", type = IdType.AUTO)
<#else>
	@TableField(value = "${field.name!}")
</#if>
<#else>
	@TableField(value = "${field.name!}")
</#if>
</#if>
	@Schema(name = "${(field.propertyName!'')?j_string}", description = <#if field.comment?has_content>"${field.comment?j_string}"<#else>"${(field.name!'')?j_string}"</#if>, required = <#if field.isNullAble()>false <#else> true </#if>)
<#if !field.commonField || (superEntityClass?? && superEntityClass?contains('.'))>
<#if !field.isNullAble()>
<#if field.fieldType.type == "String">    @NotBlank
<#else>    @NotNull
</#if>
</#if>
    private ${field.fieldType.type!} ${field.propertyName!};
    
</#if>    
</#list>
<#--  外键关联配置与引用对象机制说明： -->
<#--  -->
<#--  使用说明： -->
<#--  在此处添加外键关联的对象属性，用于关联查询和级联操作 -->
<#--  -->
<#--  一对一关系示例： -->
<#--  当前为 Order 订单类，一个订单对应一个 Person，则添加： -->
<#--      private Person person; -->
<#--  -->
<#--  多对一关系示例： -->
<#--  当前为 OrderItem 订单明细类，多个明细对应一个 Order，则添加： -->
<#--      private Order order; -->
<#--  -->
<#--  一对多关系示例： -->
<#--  当前为 Person 类，一个人对应多个 Order，则添加： -->
<#--      private List<Order> orders; -->
<#--      // 注意：一对多通常使用 @TableField(exist = false) 标注，避免映射到数据库 -->
<#--      // 查询时需要使用额外的 SQL 或 MyBatis 的 collection 映射 -->
<#--  -->
<#--  多对多关系示例： -->
<#--  当前为 Student 学生类，学生与 Course 课程多对多，则添加： -->
<#--      private List<Course> courses; -->
<#--      // 注意：多对多关系需要中间表，通常使用 @TableField(exist = false) -->
<#--      // 需要在 Mapper 中定义额外的查询方法或使用 @Many 注解 -->
<#--  -->
<#--  注意事项： -->
<#--  1. 添加关联对象属性时，应使用 @TableField(exist = false) 标注非数据库字段 -->
<#--  2. 对于 MyBatis-Plus，可以使用 @TableField(select = false) 避免自动查询 -->
<#--  3. 复杂的关联查询建议在 Mapper XML 中定义，避免 N+1 查询问题 -->
<#--  4. 级联操作（级联删除、级联更新）需要在 Service 层处理，不建议在实体类中直接处理 -->

<#list table.fields as field>
    <#if field.propertyType?string == "Boolean">
    <#assign getprefix ="is">
    <#else>
    <#assign getprefix ="get">
    </#if>

    public ${field.fieldType.type!} ${getprefix!}${field.capitalName!}() {
        return ${field.propertyName!};
    }

    public void set${field.capitalName!}(${field.fieldType.type!} ${field.propertyName!}) {
        this.${field.propertyName!} = ${field.propertyName!};
    }
</#list>
<#if sqlType?? && sqlType == "plus">
<#if table.primaryKeyCount == 1>
	@Override
	public ${table.primaryKeyField.propertyType!} pkVal() {
	    return this.${table.primaryKeyField.propertyName!};
	}
</#if>
	public String toString() {
	    return "${className!}{" +
		<#list table.fields as field>
		"<#if field_index gt 0>, </#if>${field.propertyName!}="+${field.propertyName!}+
		</#list> "}";
	}
</#if>
}

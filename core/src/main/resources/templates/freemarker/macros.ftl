<#--  ============================================ -->
<#--  Velocity 宏模板库 - 集中管理常用宏 -->
<#--  使用方式：#import("macros.vm") -->
<#--  ============================================ -->

<#--  -------------------------------------------- -->
<#--  文件头注释宏 -->
<#--  参数：table - 表对象，author - 作者，copyright - 版权信息 -->
<#--  -------------------------------------------- -->
<#macro fileHeader table author copyright>
/**
<#if table.comment?has_content  &&  "$!{table.comment}" != "null">
 * ${table.comment!}
<<<#else>>>
 * ${table.beanName!} 实体类
</#if>
 *
 * @author ${author!}
 * @since ${date!}
 * @copyright ${copyright!}
 */
</#macro>

<#--  -------------------------------------------- -->
<#--  简化版文件头 (不带表信息) -->
<#--  参数：description - 描述，author - 作者，copyright - 版权信息 -->
<#--  -------------------------------------------- -->
<#macro simpleHeader description author copyright>
/**
 * ${description!}
 *
 * @author ${author!}
 * @since ${date!}
 * @copyright ${copyright!}
 */
</#macro>

<#--  -------------------------------------------- -->
<#--  字段注释宏 -->
<#--  参数：field - 字段对象 -->
<#--  -------------------------------------------- -->
<#macro fieldComment field>
<#if field.comment  &&  field.comment != "">
    /**
     * ${field.comment!}
     */
</#if>
</#macro>

<#--  -------------------------------------------- -->
<#--  导入语句宏 - 列表 -->
<#--  参数：importList - 导入类名列表 -->
<#--  -------------------------------------------- -->
<#macro importList importList>
<#list importList as import>
<#if import  &&  import != ""  &&  import != "null">
import ${import!};
</#if>
</#list>
</#macro>

<#--  -------------------------------------------- -->
<#--  包声明宏 -->
<#--  参数：packageName - 包名 -->
<#--  -------------------------------------------- -->
<#macro declarePackage packageName>
package ${packageName!};

</#macro>

<#--  -------------------------------------------- -->
<#--  serialVersionUID 常量 -->
<#--  参数：value - 序列号值 -->
<#--  -------------------------------------------- -->
<#macro serialVersionUID value>
    private static final long serialVersionUID = ${value!}L;

</#macro>

<#--  -------------------------------------------- -->
<#--  Lombok @Data 注解 -->
<#--  -------------------------------------------- -->
<#macro lombokData >
@Data
</#macro>

<#--  -------------------------------------------- -->
<#--  Lombok @EqualsAndHashCode 注解 -->
<#--  -------------------------------------------- -->
<#macro lombokEqualsAndHashCode callSuper>
@EqualsAndHashCode(<#if callSuper>callSuper = true<<<#else>>> callSuper = false</#if>)
</#macro>

<#--  -------------------------------------------- -->
<#--  Lombok @NoArgsConstructor 注解 -->
<#--  -------------------------------------------- -->
<#macro lombokNoArgsConstructor >
@NoArgsConstructor
</#macro>

<#--  -------------------------------------------- -->
<#--  Lombok @AllArgsConstructor 注解 -->
<#--  -------------------------------------------- -->
<#macro lombokAllArgsConstructor >
@AllArgsConstructor
</#macro>

<#--  -------------------------------------------- -->
<#--  Lombok @Builder 注解 -->
<#--  -------------------------------------------- -->
<#macro lombokBuilder >
@Builder
</#macro>

<#--  -------------------------------------------- -->
<#--  MyBatis-Plus @TableName 注解 -->
<#--  参数：tableName - 表名 -->
<#--  -------------------------------------------- -->
<#macro mybatisTableName tableName>
@TableName("${tableName!}")
</#macro>

<#--  -------------------------------------------- -->
<#--  JPA @Entity 注解 -->
<#--  参数：tableName - 表名 -->
<#--  -------------------------------------------- -->
<#macro jpaEntity tableName>
@Entity
@Table(name = "${tableName!}")
</#macro>

<#--  -------------------------------------------- -->
<#--  Swagger @ApiModel 注解 -->
<#--  参数：description - 描述 -->
<#--  -------------------------------------------- -->
<#macro swaggerApiModel description>
@ApiModel("${description!}")
</#macro>

<#--  -------------------------------------------- -->
<#--  Swagger @ApiModelProperty 注解 -->
<#--  参数：description - 描述，required - 是否必填 -->
<#--  -------------------------------------------- -->
<#macro swaggerApiModelProperty description required>
@ApiModelProperty(value = "${description!}"<#if required>, required = true</#if>)
</#macro>

<#--  -------------------------------------------- -->
<#--  Spring @RestController 注解 -->
<#--  -------------------------------------------- -->
<#macro springRestController >
@RestController
</#macro>

<#--  -------------------------------------------- -->
<#--  Spring @RequestMapping 注解 -->
<#--  参数：path - 路径 -->
<#--  -------------------------------------------- -->
<#macro springRequestMapping path>
@RequestMapping("${path!}")
</#macro>

<#--  -------------------------------------------- -->
<#--  Spring @Autowired 注解 -->
<#--  -------------------------------------------- -->
<#macro springAutowired >
@Autowired
</#macro>

<#--  -------------------------------------------- -->
<#--  字段定义宏 (包含注释、注解、类型、名称) -->
<#--  参数：field - 字段对象 -->
<#--  -------------------------------------------- -->
<#macro defineField field>
    #fieldComment(${field!})
    private ${field.fieldType.type!} ${field.propertyName!};

</#macro>

<#--  -------------------------------------------- -->
<#--  Getter 方法宏 -->
<#--  参数：field - 字段对象 -->
<#--  -------------------------------------------- -->
<#macro getterMethod field>
    public ${field.fieldType.type!} get${field.capitalName!}() {
        return ${field.propertyName!};
    }

</#macro>

<#--  -------------------------------------------- -->
<#--  Setter 方法宏 -->
<#--  参数：field - 字段对象 -->
<#--  -------------------------------------------- -->
<#macro setterMethod field>
    public void set${field.capitalName!}(${field.fieldType.type!} ${field.propertyName!}) {
        this.${field.propertyName!} = ${field.propertyName!};
    }

</#macro>

<#--  -------------------------------------------- -->
<#--  Getter/Setter 方法对宏 -->
<#--  参数：field - 字段对象 -->
<#--  -------------------------------------------- -->
<#macro getterAndSetter field>
#getterMethod(${field!})
#setterMethod(${field!})
</#macro>

<#--  -------------------------------------------- -->
<#--  equals 方法 (复合主键使用) -->
<#--  参数：className - 类名，fields - 字段列表 -->
<#--  -------------------------------------------- -->
<#macro equalsMethod className fields>
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ${className!} that = (${className!}) o;
        return <#list fields as field>Objects.equals(${field.propertyName!}, that.${field.propertyName!})<#if item_index + 1  lt  fields.size()> && </#if></#list>;
    }

</#macro>

<#--  -------------------------------------------- -->
<#--  hashCode 方法 (复合主键使用) -->
<#--  参数：fields - 字段列表 -->
<#--  -------------------------------------------- -->
<#macro hashCodeMethod fields>
    @Override
    public int hashCode() {
        return Objects.hash(<#list fields as field>${field.propertyName!}<#if item_index + 1  lt  fields.size()>, </#if></#list>);
    }

</#macro>

<#--  -------------------------------------------- -->
<#--  toString 方法 -->
<#--  参数：beanName - 类名，fields - 字段列表 -->
<#--  -------------------------------------------- -->
<#macro toStringMethod beanName fields>
    @Override
    public String toString() {
        return "${beanName!}{" +
<#list fields as field>
            "${field.propertyName!}=" + ${field.propertyName!}<#if item_index + 1  lt  fields.size()> + ", " + </#if>
</#list>
            + "}";
    }
</#macro>

<#--  -------------------------------------------- -->
<#--  判断是否为空字符串 -->
<#--  参数：str - 字符串 -->
<#--  返回：true/false -->
<#--  -------------------------------------------- -->
<#macro isEmpty str>
<#if "$!str" == ""  ||  "$!str" == "null">true<<<#else>>> false</#if>
</#macro>

package ${entityPackage!};

<#list table.importPackages as pkg>
import ${pkg!};
</#list>
import java.io.Serializable;    
import ${persistencePackage}.Entity;
import ${persistencePackage}.GeneratedValue;
import ${persistencePackage}.GenerationType;
import ${persistencePackage}.Id;
import ${validationPackage}.constraints.NotBlank;
import ${validationPackage}.constraints.NotNull;

<#include 'comments/comment.ftl'>
@Entity
public class ${className!} implements Serializable{

    private static final long serialVersionUID = 1L;

<#list table.fields as field>
<#if field.comment?has_content>
    /**
    * ${field.comment!}
    */
</#if>
<#if field.isPrimarykey()>
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
</#if>
<#if !field.isNullAble() && !field.isPrimarykey()>
<#if field.fieldType.type == "String">    @NotBlank
<#else>    @NotNull
</#if>
</#if>
    private ${field.fieldType.type!} ${field.propertyName!};
</#list>

<#list table.fields as field>
    <#if field.propertyType.equals("Boolean")>
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
}

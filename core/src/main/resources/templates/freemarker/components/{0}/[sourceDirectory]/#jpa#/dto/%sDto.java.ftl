package ${dtoPackage!};

import java.io.Serializable;
<#list table.importPackages as pkg>
<#if pkg?has_content && !pkg?contains('jakarta.validation') && !pkg?contains('org.apache.commons')>
import ${pkg!};
</#if>
</#list>

import io.swagger.v3.oas.annotations.media.Schema;

<#include 'comments/comment.ftl'>
<#if table.comment??>
@Schema(name = "${dtoName!}", description = "${table.comment!}")
</#if>
public class ${dtoName!} implements Serializable {

    private static final long serialVersionUID = 1L;

<#list table.fields as field>

<#if field.comment?has_content>

    /**
    * ${field.comment!}
    */

</#if>

	@Schema(name = "${field.propertyName!}", description = <#if field.comment?has_content>"${field.comment!}"<#else>"${field.name!}"</#if>)
    private ${field.fieldType.type!} ${field.propertyName!};
    
</#list>

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

	public String toString() {
	    return "${dtoName!}{" +
		<#list table.fields as field>
		"<#if field_index gt 0>, </#if>${field.propertyName!}="+${field.propertyName!}+
		</#list> "}";
	}
}
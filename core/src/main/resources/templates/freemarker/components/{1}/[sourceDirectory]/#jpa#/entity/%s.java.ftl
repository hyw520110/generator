package ${entityPackage!};

<#list table.importPackages as pkg>
import ${pkg!};
</#list>
import java.io.Serializable;    
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

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
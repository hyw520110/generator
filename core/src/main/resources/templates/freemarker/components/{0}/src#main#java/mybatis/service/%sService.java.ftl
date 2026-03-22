package ${servicePackage!};

import ${entityPackage!}.${entityName!};
<#if superServiceClass?? && !superServiceClass?contains('.')>
import ${servicePackage!}.${superServiceClass!};
</#if>

<#include 'comments/comment.ftl'>
public interface ${className!} <#if superServiceClass??> extends ${superServiceClass!}<${entityName!}> </#if>{
}
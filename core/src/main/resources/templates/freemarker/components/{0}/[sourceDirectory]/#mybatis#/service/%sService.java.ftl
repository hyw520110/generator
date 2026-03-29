package ${servicePackage!};

import ${dtoPackage!}.${dtoName!};
<#if superServiceClass?? && !superServiceClass?contains('.')>
import ${servicePackage!}.${superServiceClass!};
</#if>

<#include 'comments/comment.ftl'>
public interface ${serviceName!} <#if superServiceClass??> extends ${superServiceClass!}<${dtoName!}> </#if>{
}
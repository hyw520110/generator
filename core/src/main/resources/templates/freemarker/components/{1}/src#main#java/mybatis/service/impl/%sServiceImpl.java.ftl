package ${implPackage!};

import ${entityPackage!}.${entityName!};
import ${mapperPackage!}.${mapperName!};
import ${servicePackage!}.${serviceName!};
<#if superServiceImplClass?? && !superServiceImplClass?contains('.')>
import ${implPackage!}.${superServiceImplClass!};
</#if>
import org.springframework.beans.factory.annotation.Autowired;

<#include 'comments/comment.ftl'>
@org.springframework.stereotype.Service
<#if DUBBO?? && DUBBO>
@org.apache.dubbo.config.annotation.DubboService
</#if>
public class ${className!} <#if superServiceImplClass??> extends ${superServiceImplClass!}<${mapperName!},${entityName!}> </#if> implements ${serviceName!} {
<#if "plus"!=mapperType!>
<#assign sName = StringUtils.lowercaseFirst(mapperName)!>

	@Autowired
	private ${mapperName!} ${sName!};
	
</#if>
}

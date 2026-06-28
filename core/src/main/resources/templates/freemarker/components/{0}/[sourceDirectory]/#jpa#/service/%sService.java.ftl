package ${servicePackage!};

import ${entityPackage!}.${entityName!};
<#if table.hasPrimarykeys()>
<#if table.primaryKeyField.fieldType.claz?has_content> 
import ${table.primaryKeyField.fieldType.claz!};	
</#if>	
</#if>

<#include 'comments/comment.ftl'>
public interface ${serviceName!} extends BaseJpaService<${entityName!},${table.primaryKeyClass!}> {
	
}

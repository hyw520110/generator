package ${servicePackage!};

import ${entityPackage!}.${entityName!};
<#if table.primaryKeyField.fieldType.claz> 
import ${table.primaryKeyField.fieldType.claz!};	
</#if>	

<#include 'comments/comment.ftl'>
public interface ${serviceName!} extends BaseJpaService<${entityName!},${table.primaryKeyField.fieldType.type!}> {
	
}

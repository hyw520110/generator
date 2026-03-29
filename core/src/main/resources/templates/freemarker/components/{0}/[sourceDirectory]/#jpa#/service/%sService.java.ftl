package ${servicePackage!};

import ${dtoPackage!}.${dtoName!};
<#if table.primaryKeyField.fieldType.claz> 
import ${table.primaryKeyField.fieldType.claz!};	
</#if>	

<#include 'comments/comment.ftl'>
public interface ${serviceName!} extends BaseJpaService<${dtoName!},${table.primaryKeyField.fieldType.type!}> {
	
}

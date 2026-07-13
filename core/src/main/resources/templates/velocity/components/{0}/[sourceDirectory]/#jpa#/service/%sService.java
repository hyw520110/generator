package ${servicePackage};

import ${entityPackage}.${entityName};
#if($table.hasPrimarykeys())
#if("$!table.primaryKeyField.fieldType.claz" != "") 
import ${table.primaryKeyField.fieldType.claz};	
#end	
#end

#parse('/templates/comments/comment.vm')
public interface ${serviceName} extends BaseJpaService<${entityName},${table.primaryKeyClass}> {
	
}

package ${servicePackage};

import ${entityPackage}.${entityName};
#if(${table.primaryKeyField.fieldType.claz}) 
import ${table.primaryKeyField.fieldType.claz};	
#end	

#parse('/templates/comments/comment.vm')
public interface ${className} extends BaseJpaService<${entityName},${table.primaryKeyField.fieldType.type}> {
	
}

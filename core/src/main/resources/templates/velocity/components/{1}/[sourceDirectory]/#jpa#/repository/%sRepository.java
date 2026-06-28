package ${repositoryPackage};

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;


import ${entityPackage}.${entityName};
#if($table.hasPrimarykeys())
#if(${table.primaryKeyField.fieldType.claz}) 
import ${table.primaryKeyField.fieldType.claz};	
#end	
#end
#if(${superRepositoryClass})
import #if($StringUtils.indexOf("$superRepositoryClass",'.')==-1)${mapperPackage}.#end${superRepositoryClass};
#end

#parse('/templates/comments/comment.vm')
@Repository
public interface ${repositoryName} #if(${superRepositoryClass}) extends ${StringUtils.getClassName(${superRepositoryClass})} #else extends CrudRepository<${entityName},${table.primaryKeyClass}> #end{

}

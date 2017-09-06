package ${repositoryPackage};

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;


import ${entityPackage}.${entityName};
#if(${superRepositoryClass})
import #if($StringUtils.indexOf("$superRepositoryClass",'.')==-1)${mapperPackage}.#end${superRepositoryClass};
#end

#parse('/templates/commons/comment.vm')
@Repository
public interface ${className} #if(${superRepositoryClass}) extends ${StringUtils.getClassName(${superRepositoryClass})} #else extends CrudRepository #end{

}
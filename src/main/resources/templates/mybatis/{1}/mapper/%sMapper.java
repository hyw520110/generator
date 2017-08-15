package ${mapperPackage};

import org.springframework.stereotype.Repository;

import ${entityPackage}.${entityName};
#if(${superMapperClass})
import #if($StringUtils.indexOf("$superMapperClass",'.')==-1)${mapperPackage}.#end${superMapperClass};
#end


#parse('/templates/commons/comment.vm')
//TODO 泛型 联合主键处理<${className}>
@Repository
public interface ${className} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})}<${StringUtils.capitalFirst("$entityName")},${table.primaryKeyField.fieldType.type}> #end{

}
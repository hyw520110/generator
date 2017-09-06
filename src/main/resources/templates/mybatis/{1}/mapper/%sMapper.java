package ${mapperPackage};

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import ${entityPackage}.${entityName};
#if(${superMapperClass})
import #if($StringUtils.indexOf("$superMapperClass",'.')==-1)${mapperPackage}.#end${superMapperClass};
#end


#parse('/templates/commons/comment.vm')
@Repository
public interface ${className} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})}<${StringUtils.capitalFirst("$entityName")}> #end{

	public ${StringUtils.capitalFirst("$entityName")} findById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
	
	public Integer deleteById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);

}
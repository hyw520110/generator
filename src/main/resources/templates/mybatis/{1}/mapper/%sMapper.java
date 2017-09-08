package ${mapperPackage};

import org.apache.ibatis.annotations.Param;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import ${entityPackage}.${entityName};
#if(${superMapperClass})
import #if($StringUtils.indexOf("$superMapperClass",'.')==-1)${mapperPackage}.#end${superMapperClass};
#end
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
#parse('/templates/commons/comment.vm')

@Repository
@CacheConfig(cacheNames = "${StringUtils.lowercaseFirst($className)}")
public interface ${className} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})}<${StringUtils.capitalFirst("$entityName")}> #end{

    @Cacheable(key = "#p0")
	public ${StringUtils.capitalFirst("$entityName")} findById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
	
	@CacheEvict(key = "#p0")
	public Integer deleteById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);

}
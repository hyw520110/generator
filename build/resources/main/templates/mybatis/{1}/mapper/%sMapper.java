package ${mapperPackage};

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import ${entityPackage}.${entityName};
#if(${superMapperClass})
import #if($StringUtils.indexOf("$superMapperClass",'.')==-1)${mapperPackage}.#end${superMapperClass};
#end
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
#if("${sqlType}"=="SQL")
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
#end
#parse('/templates/commons/comment.vm')

#if("${sqlType}"=="SQL")
@Mapper
#else
@Repository
@CacheConfig(cacheNames = "${StringUtils.lowercaseFirst($className)}")
#end
public interface ${className} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})}<${StringUtils.capitalFirst("$entityName")}> #end{
    #if("${sqlType}"=="SQL")
        //TODO 
    @Select("SELECT * FROM ${table.beanName} WHERE #foreach($field in ${table.primarykeyFields})${field.name} = #{${field.propertyName}}#if($foreach.count!=${table.primarykeyFields.size()}) and #end#end")
    #end
    @Cacheable(key = "#p0")
	public ${StringUtils.capitalFirst("$entityName")} findById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
	
	@CacheEvict(key = "#p0")
	public Integer deleteById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);

}
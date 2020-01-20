package ${mapperPackage};

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import ${entityPackage}.${entityName};
#if(${superMapperClass})
#if("${mapperType}"=="plus")
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
#else
import #if($StringUtils.indexOf("$superMapperClass",'.')==-1)${mapperPackage}.#end${superMapperClass};
#end
#end
#if($!{REDIS}&&"${mapperType}"!="plus")
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
#end
#if("${mapperType}"=="SQL")
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
#end
#parse('/templates/commons/comment.vm')

#if("${mapperType}"=="SQL")
@Mapper
#end
#if("annotation"=="$mapperType")
@Repository
#end
#if($!{REDIS}&&"plus"!="$mapperType")
@CacheConfig(cacheNames = "${StringUtils.lowercaseFirst($className)}")
#end
public interface ${className} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})}<${StringUtils.capitalFirst("$entityName")}> #end{
#if("${mapperType}"!="plus")
#if("${mapperType}"=="annotation")
        //TODO 
    @Select("SELECT * FROM ${table.beanName} WHERE #foreach($field in ${table.primarykeyFields})${field.name} = #{${field.propertyName}}#if($foreach.count!=${table.primarykeyFields.size()}) and #end#end")
#end
#if($!{REDIS})
    @Cacheable(key = "#p0")
#end
	public ${StringUtils.capitalFirst("$entityName")} findById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
	
#if($!{REDIS})
    @CacheEvict(key = "#p0")
#end
	public Integer deleteById(#foreach($field in ${table.primarykeyFields})@Param("${field.propertyName}")${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
#end
}
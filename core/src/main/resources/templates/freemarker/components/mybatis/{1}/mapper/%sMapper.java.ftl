package ${mapperPackage!};

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import ${entityPackage!}.${entityName!};
<#if superMapperClass??>
<#if mapperType=="plus">
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
<#else>
<#if superMapperClass?contains('.')>
import ${superMapperClass!};
<#else>
import ${mapperPackage!}.${superMapperClass!};
</#if>
</#if>
</#if>
<#if REDIS?has_content && mapperType!="plus">
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
</#if>
<#if mapperType=="SQL">
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
</#if>

<#include 'comments/comment.ftl'>
<#if mapperType=="SQL">
@Mapper
</#if>
<#if "annotation"=="mapperType">
@Repository
</#if>
<#if REDIS?has_content && "plus"!=mapperType>
@CacheConfig(cacheNames = "${entityName?lower_case}")
</#if>
public interface ${className!} <#if superMapperClass??> extends ${superMapperClass!}<${entityName}><#if "plus"!=mapperType>,${table.primaryKeyClass!}</#if>> </#if>{

<#if "plus"!=mapperType>
<#if "annotation"==mapperType>
        //TODO
<#assign conditions = []>
<#list table.primarykeyFields as field>
<#assign conditions = conditions + ["${field.name!} = " + "#{" + field.propertyName + "}"]>
</#list>
<#assign whereClause = conditions?join(" and ")>
    @Select("SELECT * FROM " + table.beanName + " WHERE " + whereClause)
</#if>
<#if REDIS?has_content>
    @Cacheable(key = "#p0")
</#if>
	public ${entityName!} findById(<#list table.primarykeyFields as field>@Param("${field.propertyName!}")${field.fieldType.type!} ${field.propertyName!}<#if field?has_next>,</#if></#list>);
	
<#if REDIS?has_content>
    @CacheEvict(key = "#p0")
	public void deleteById(<#list table.primarykeyFields as field>@Param("${field.propertyName!}")${field.fieldType.type!} ${field.propertyName!}<#if field?has_next>,</#if></#list>);
</#if>

}
</#if>
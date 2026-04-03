package ${mapperPackage!};

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import ${entityPackage!}.${entityName!};
<#if superMapperClass?? && superMapperClass?has_content>
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
<#if REDIS?? && mapperType!="plus">
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheConfig;
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
<#if "annotation"==mapperType!>
@Repository
</#if>
<#if REDIS?? && "plus"!=mapperType!>
@CacheConfig(cacheNames = "${entityName!?lower_case}")
</#if>
public interface ${mapperName!} <#if superMapperClass?? && superMapperClass?has_content> extends ${superMapperClass!}<${entityName!}> </#if>{

<#if "plus"!=mapperType!>
<#if REDIS??>
    @Cacheable(key = "#p0")
</#if>
	public ${entityName!} findById(<#list table.primarykeyFields as field>@Param("${field.propertyName!}")${field.fieldType.type!} ${field.propertyName!}<#if field?has_next>,</#if></#list>);
	
<#if REDIS??>
    @CacheEvict(key = "#p0")
	public void deleteById(<#list table.primarykeyFields as field>@Param("${field.propertyName!}")${field.fieldType.type!} ${field.propertyName!}<#if field?has_next>,</#if></#list>);
</#if>
</#if>
}
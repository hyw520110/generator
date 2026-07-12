package ${mapperPackage!};

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import ${entityPackage!}.${entityName!};
<#if superMapperClass?? && superMapperClass?has_content>
<#if sqlType=="plus">
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
<#else>
<#if superMapperClass?contains('.')>
import ${superMapperClass!};
<#else>
import ${mapperPackage!}.${superMapperClass!};
</#if>
</#if>
</#if>
<#if REDIS?? && sqlType!="plus">
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
</#if>
<#if sqlType=="SQL">
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
</#if>

<#include 'comments/comment.ftl'>
<#if sqlType=="SQL">
@Mapper
</#if>
<#if "annotation"==sqlType!>
@Repository
</#if>
<#if REDIS?? && "plus"!=sqlType!>
@CacheConfig(cacheNames = "${entityName!?lower_case}")
</#if>
public interface ${mapperName!} <#if superMapperClass?? && superMapperClass?has_content> extends ${superMapperClass!}<${entityName!}> </#if>{

<#if "plus"!=sqlType!>
<#if table.hasPrimarykeys()>
<#if REDIS??>
    @Cacheable(key = "#p0")
</#if>
	public ${entityName!} findById(<#list table.primarykeyFields as field>@Param("${field.propertyName!}")${field.fieldType.type!} ${field.propertyName!}<#if field?has_next>,</#if></#list>);
	
<#if REDIS??>
    @CacheEvict(key = "#p0")
	public void deleteById(<#list table.primarykeyFields as field>@Param("${field.propertyName!}")${field.fieldType.type!} ${field.propertyName!}<#if field?has_next>,</#if></#list>);
</#if>
</#if>
</#if>
}

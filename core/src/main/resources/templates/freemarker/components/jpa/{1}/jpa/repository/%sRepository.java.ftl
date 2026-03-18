package ${repositoryPackage!};

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;


import ${entityPackage!}.${entityName!};
<#if table.primaryKeyField.fieldType.claz> 
import ${table.primaryKeyField.fieldType.claz!};	
</#if>	
<#if superRepositoryClass>
import <#if StringUtils.indexOf("${superRepositoryClass}",'.')==-1>${mapperPackage!}.</#if>${superRepositoryClass!};
</#if>

<#include 'comments/comment.ftl'>
@Repository
public interface ${className!} <#if superRepositoryClass> extends ${StringUtils.getClassName(${superRepositoryClass})!}!}!} <<<#else>>> extends CrudRepository<${entityName!},${table.primaryKeyField.fieldType.type!}> </#if>{

}
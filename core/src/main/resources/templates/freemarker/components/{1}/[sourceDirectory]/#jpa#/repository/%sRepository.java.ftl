package ${repositoryPackage!};

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;


import ${entityPackage!}.${entityName!};
<#if table.primaryKeyField.fieldType.claz?has_content> 
import ${table.primaryKeyField.fieldType.claz!};	
</#if>	
<#if superRepositoryClass?has_content>
import <#if StringUtils.indexOf(superRepositoryClass!,'.')==-1>${mapperPackage!}.</#if>${superRepositoryClass!};
</#if>

<#include 'comments/comment.ftl'>
@Repository
public interface ${repositoryName!} <#if superRepositoryClass?has_content> extends ${StringUtils.getClassName(superRepositoryClass!)}<#else> extends CrudRepository<${entityName!},${table.primaryKeyField.fieldType.type!}> </#if>{

}
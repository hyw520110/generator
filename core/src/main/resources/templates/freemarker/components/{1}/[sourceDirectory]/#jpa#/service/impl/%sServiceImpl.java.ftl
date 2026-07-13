package ${implPackage!};

import ${entityPackage!}.${entityName!};
<#if table.hasPrimarykeys()>
<#if table.primaryKeyField.fieldType.claz?has_content> 
import ${table.primaryKeyField.fieldType.claz!};	
</#if>
</#if>
import ${repositoryPackage!}.${repositoryName!};
import ${servicePackage!}.${serviceName!};
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

<#include 'comments/comment.ftl'>
@Service
<#if DUBBO?? && DUBBO>@org.apache.dubbo.config.annotation.DubboService
</#if>
public class ${implName!} implements ${serviceName!} {
	@Autowired
	private ${repositoryName!} repository;
	
	public boolean save(${entityName!} entity) {
		return null != repository.save(entity);
	}

<#if table.hasPrimarykeys()>
	public ${entityName!} findById(${table.primaryKeyClass!} id) {
		Optional<${entityName!}> opt = repository.findById(id);
		return opt.orElse(null);
	}

	public Boolean deleteById(${table.primaryKeyClass!} id) {
		try {
			repository.deleteById(id);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
<#else>
    public ${entityName!} findById(Serializable id) { return null; }
    public Boolean deleteById(Serializable id) { return false; }
</#if>

	public Long count() {
		return repository.count();
	}
	
	public Iterable<${entityName!}> findAll() {
		return repository.findAll();
	}
}

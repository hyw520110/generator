package ${implPackage!};

import ${dtoPackage!}.${dtoName!};
import ${entityPackage!}.${entityName!};
<#if table.primaryKeyField.fieldType.claz> 
import ${table.primaryKeyField.fieldType.claz!};	
</#if>	
import ${repositoryPackage!}.${repositoryName!};
import ${servicePackage!}.${serviceName!};
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


<#include 'comments/comment.ftl'>
@Service
<#if DUBBO?? && DUBBO>@org.apache.dubbo.config.annotation.DubboService
</#if>
public class ${implName!}  implements ${serviceName!} {
	@Autowired
	private ${repositoryName!} repository;
	
	public boolean save(${dtoName!} dto) {
		${entityName!} entity = toEntity(dto);
		return null!=repository.save(entity);
	}

	public ${dtoName!} findById(${table.primaryKeyField.fieldType.type!} id) {
		${entityName!} entity = repository.findOne(id);
		return toDto(entity);
	}

	public Long count() {
		return repository.count();
	}
	
	public List<${dtoName!}> findAll() {
		Iterable<${entityName!}> entities = repository.findAll();
		List<${dtoName!}> dtos = new ArrayList<>();
		entities.forEach(entity -> dtos.add(toDto(entity)));
		return dtos;
	}
	
	public Boolean deleteById(${table.primaryKeyField.fieldType.type!} id) {
		try {
			repository.delete(id);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * DTO转Entity
	 */
	private ${entityName!} toEntity(${dtoName!} dto) {
		if (dto == null) return null;
		${entityName!} entity = new ${entityName!}();
		BeanUtils.copyProperties(dto, entity);
		return entity;
	}
	
	/**
	 * Entity转DTO
	 */
	private ${dtoName!} toDto(${entityName!} entity) {
		if (entity == null) return null;
		${dtoName!} dto = new ${dtoName!}();
		BeanUtils.copyProperties(entity, dto);
		return dto;
	}
}
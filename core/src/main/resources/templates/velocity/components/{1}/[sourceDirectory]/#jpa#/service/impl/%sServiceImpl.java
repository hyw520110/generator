package ${implPackage};

import ${dtoPackage}.${dtoName};
import ${entityPackage}.${entityName};
#if(${table.primaryKeyField.fieldType.claz}) 
import ${table.primaryKeyField.fieldType.claz};	
#end	
import ${repositoryPackage}.${repositoryName};
import ${servicePackage}.${serviceName};
import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


#parse('/templates/comments/comment.vm')
@Service
#if($!{springboot_dubbo_version})@com.alibaba.dubbo.config.annotation.Service
#end
public class ${implName}  implements ${serviceName} {
	@Autowired
	private ${repositoryName} repository;
	
	public boolean save(${dtoName} dto) {
		${entityName} entity = toEntity(dto);
		return null!=repository.save(entity);
	}

	public ${dtoName} findById(${table.primaryKeyField.fieldType.type} id) {
		${entityName} entity = repository.findOne(id);
		return toDto(entity);
	}

	public Long count() {
		return repository.count();
	}
	public Iterable<${dtoName}> findAll() {
		Iterable<${entityName}> entities = repository.findAll();
		return toDtoList(entities);
	}
	public Boolean deleteById(${table.primaryKeyField.fieldType.type} id) {
		  try {
			  repository.delete(id);
		} catch (Exception e) {
			return false;
		}
		  return true;
	}
	
	private ${entityName} toEntity(${dtoName} dto) {
		if (dto == null) return null;
		${entityName} entity = new ${entityName}();
#foreach($field in $table.fields)
		entity.set${field.capitalName}(dto.get${field.capitalName}());
#end
		return entity;
	}
	
	private ${dtoName} toDto(${entityName} entity) {
		if (entity == null) return null;
		${dtoName} dto = new ${dtoName}();
#foreach($field in $table.fields)
		dto.set${field.capitalName}(entity.get${field.capitalName}());
#end
		return dto;
	}
	
	private Iterable<${dtoName}> toDtoList(Iterable<${entityName}> entities) {
		java.util.List<${dtoName}> dtos = new java.util.ArrayList<>();
		for (${entityName} entity : entities) {
			dtos.add(toDto(entity));
		}
		return dtos;
	}
}
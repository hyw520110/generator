package ${implPackage};

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
#if($!{spring_boot_dubbo_version})@com.alibaba.dubbo.config.annotation.Service
#end
public class ${className}  implements ${serviceName} {
	@Autowired
	private ${repositoryName} repository;
	
	public boolean save(${entityName} entity) {
		return null!=repository.save(entity);
	}

	public ${entityName} findById(${table.primaryKeyField.fieldType.type} id) {
		return repository.findOne(id);
	}

	public Long count() {
		return repository.count();
	}
	public Iterable<${entityName}> findAll() {
		return repository.findAll();
	}
	public Boolean deleteById(${table.primaryKeyField.fieldType.type} id) {
		  try {
			  repository.delete(id);
		} catch (Exception e) {
			return false;
		}
		  return true;
	}
	
}

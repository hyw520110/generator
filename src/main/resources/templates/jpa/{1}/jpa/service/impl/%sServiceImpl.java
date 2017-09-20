package ${implPackage};

import ${entityPackage}.${entityName};
import ${repositoryPackage}.${repositoryName};
import ${servicePackage}.${serviceName};
import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


#parse('/templates/commons/comment.vm')
@Service
#if($!{spring_boot_dubbo_version})@com.alibaba.dubbo.config.annotation.Service
#end
public class ${className}  implements ${serviceName} {
	 //TODO 
	@Autowired
	private ${repositoryName} repository;
	@Override
	public boolean save(Object entity) {
		return null!=repository.save(entity);
	}
	@Override
	public Object findById(Serializable id) {
		return repository.findOne(id);
	}

	public Long count() {
		return repository.count();
	}
	public Iterable<${entityName}> findAll() {
		return repository.findAll();
	}
	public Boolean deleteById(Serializable id) {
		  try {
			  repository.delete(id);
		} catch (Exception e) {
			return false;
		}
		  return true;
	}
	
}

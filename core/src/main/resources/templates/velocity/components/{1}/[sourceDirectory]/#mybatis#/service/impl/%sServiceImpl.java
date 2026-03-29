package ${implPackage};

import ${dtoPackage}.${dtoName};
import ${entityPackage}.${entityName};
import ${mapperPackage}.${mapperName};
import ${servicePackage}.${serviceName};
#if(${superServiceImplClass})
import #if($StringUtils.indexOf("$superServiceImplClass",'.')==-1)${implPackage}.#end${superServiceImplClass};
#end
#if($table.isCompositePrimaryKey())
import ${entityPackage}.key.${table.beanName}Key;
#end
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;

#parse('/templates/comments/comment.vm')
@org.springframework.stereotype.Service
#if($!{DUBBO})
@org.apache.dubbo.config.annotation.DubboService
#end
public class ${implName} #if(${superServiceImplClass}) extends ${StringUtils.getClassName(${superServiceImplClass})}<#if("plus"=="$mapperType")${StringUtils.capitalFirst("$entityName")}Mapper,${dtoName}#else ${StringUtils.capitalFirst("$entityName")},${table.primaryKeyClass}#end> #end implements ${serviceName} {
#if("plus"!="$mapperType")
#set($sName=${StringUtils.lowercaseFirst($mapperName)})

	@Autowired
	private ${mapperName} ${sName};
	
#end
	private ${entityName} toEntity(${dtoName} dto) {
		if (dto == null) return null;
		${entityName} entity = new ${entityName}();
		BeanUtils.copyProperties(dto, entity);
		return entity;
	}
	
	private ${dtoName} toDto(${entityName} entity) {
		if (entity == null) return null;
		${dtoName} dto = new ${dtoName}();
		BeanUtils.copyProperties(entity, dto);
		return dto;
	}
}
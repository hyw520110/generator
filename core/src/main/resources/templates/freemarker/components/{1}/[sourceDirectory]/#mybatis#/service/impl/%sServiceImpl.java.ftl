package ${implPackage!};

import ${dtoPackage!}.${dtoName!};
import ${entityPackage!}.${entityName!};
import ${mapperPackage!}.${mapperName!};
import ${servicePackage!}.${serviceName!};
<#if superServiceImplClass?? && !superServiceImplClass?contains('.')>
import ${implPackage!}.${superServiceImplClass!};
</#if>
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

<#include 'comments/comment.ftl'>
@org.springframework.stereotype.Service
<#if DUBBO?? && DUBBO>
@org.apache.dubbo.config.annotation.DubboService
</#if>
public class ${implName!} <#if superServiceImplClass??> extends ${superServiceImplClass!}<${mapperName!},${entityName!}> </#if> implements ${serviceName!} {
<#if "plus"!=sqlType!>
<#assign sName = StringUtils.lowercaseFirst(mapperName)!>

	@Autowired
	private ${mapperName!} ${sName!};
	
</#if>

	/**
	 * DTO转Entity
	 */
	public ${entityName!} toEntity(${dtoName!} dto) {
		if (dto == null) return null;
		${entityName!} entity = new ${entityName!}();
		BeanUtils.copyProperties(dto, entity);
		return entity;
	}
	
	/**
	 * Entity转DTO
	 */
	public ${dtoName!} toDto(${entityName!} entity) {
		if (entity == null) return null;
		${dtoName!} dto = new ${dtoName!}();
		BeanUtils.copyProperties(entity, dto);
		return dto;
	}
	
	/**
	 * Entity列表转DTO列表
	 */
	public List<${dtoName!}> toDtoList(List<${entityName!}> entities) {
		if (entities == null) return null;
		List<${dtoName!}> dtos = new ArrayList<>();
		for (${entityName!} entity : entities) {
			dtos.add(toDto(entity));
		}
		return dtos;
	}

<#if "plus"==sqlType!>
    @Override
    <#if SEATA?? && SEATA>@io.seata.spring.annotation.GlobalTransactional</#if>
    public boolean save(${entityName!} entity) {
        return super.save(entity);
    }

    @Override
    <#if MULTIDATASOURCE?? && MULTIDATASOURCE>@com.baomidou.dynamic.datasource.annotation.DS("slave")</#if>
    public List<${entityName!}> list() {
        return super.list();
    }

    @Override
    <#if MULTICACHE?? && MULTICACHE>@com.alicp.jetcache.anno.Cached(name = "${entityName!}Cache:", key = "#id", expire = 3600)</#if>
    public ${entityName!} getById(Serializable id) {
        return super.getById(id);
    }
</#if>
}

package ${implPackage};

import ${entityPackage}.${entityName};
import ${mapperPackage}.${mapperName};
import ${servicePackage}.${serviceName};
#if(${superServiceImplClass})
import #if($StringUtils.indexOf("$superServiceImplClass",'.')==-1)${implPackage}.#end${superServiceImplClass};
#end
import org.springframework.beans.factory.annotation.Autowired;

#parse('/templates/comments/comment.vm')
@org.springframework.stereotype.Service
#if($!{DUBBO})
@org.apache.dubbo.config.annotation.DubboService
#end
public class ${className} #if(${superServiceImplClass}) extends ${StringUtils.getClassName(${superServiceImplClass})}<#if("plus"=="$mapperType")${StringUtils.capitalFirst("$entityName")}Mapper,$entityName#else ${StringUtils.capitalFirst("$entityName")},${table.primaryKeyClass}#end> #end implements ${serviceName} {
#if("plus"!="$mapperType")
#set($sName=${StringUtils.lowercaseFirst($mapperName)})

	@Autowired
	private ${mapperName} ${sName};
	
#end
}

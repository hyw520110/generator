package ${implPackage};

import ${entityPackage}.${entityName};
import ${mapperPackage}.${mapperName};
import ${servicePackage}.${serviceName};
#if(${superServiceImplClass})
import #if($StringUtils.indexOf("$superServiceImplClass",'.')==-1)${implPackage}.#end${superServiceImplClass};
#end
import org.springframework.stereotype.Service;

#parse('/templates/commons/comment.vm')
@Service("${StringUtils.lowercaseFirst($serviceName)}")
public class ${className} #if(${superServiceImplClass}) extends ${StringUtils.getClassName(${superServiceImplClass})}<${StringUtils.capitalFirst("$entityName")},${table.primaryKeyField.fieldType.type}> #end implements ${serviceName} {
	
}

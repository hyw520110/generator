package ${implPackage};

import ${entityPackage}.${entityName};
import ${mapperPackage}.${mapperName};
import ${servicePackage}.${serviceName};
#if(${superServiceImplClass})
import #if($StringUtils.indexOf("$superServiceImplClass",'.')==-1)${implPackage}.#end${superServiceImplClass};
#end
import org.springframework.stereotype.Service;

/**
 * $!{table.comment} 服务实现类
 * @author ${author}
 * @since ${date}
 */
@Service("${StringUtils.lowercaseFirst($serviceName)}")
public class ${className} #if(${superServiceImplClass}) extends ${StringUtils.getClassName(${superServiceImplClass})} #end implements ${serviceName} {
	
}

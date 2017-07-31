package ${servicePackage};

import ${entityPackage}.${entityName};
#if(${superServiceClass})
import #if($StringUtils.indexOf("$superServiceClass",'.')==-1)${servicePackage}.#end${superServiceClass};
#end

/**
 * $!{table.comment} 服务类
 * @author ${author}
 * @since ${date}
 */
public interface ${className} #if(${superServiceClass}) extends ${StringUtils.getClassName(${superServiceClass})} #end{
	
}

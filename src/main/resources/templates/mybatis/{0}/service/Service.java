package ${packageService};

import ${packageEntity}.${EntityName};
#if(${superServiceClass})
import #if($StringUtils.indexOf("$superServiceClass",'.')==-1)${packageService}.#end${superServiceClass};
#end

/**
 * $!{table.comment} 服务类
 * @author ${author}
 * @since ${date}
 */
public interface ${ServiceName} #if(${superServiceClass}) extends ${StringUtils.getClassName(${superServiceClass})} #end{
	
}

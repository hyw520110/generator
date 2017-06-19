package ${packageService};

import ${packageEntity}.${EntityName};
#if(${superServiceClassName})
import ${superServiceClass};
#end

/**
 * $!{table.comment} 服务类
 * @author ${author}
 * @since ${date}
 */
public interface ${ServiceName} #if(${superServiceClassName}) extends ${superServiceClassName}<${EntityName}> #end{
	
}

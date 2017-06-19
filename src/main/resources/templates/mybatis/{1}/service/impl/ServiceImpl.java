package ${packageServiceImpl};

import ${packageEntity};
import ${packageMapper};
import ${packageService}.${ServiceName};
#if(${superServiceImplClassPackage})
import ${superServiceImplClassPackage};
#end
import org.springframework.stereotype.Service;

/**
 * $!{table.comment} 服务实现类
 * @author ${author}
 * @since ${date}
 */
@Service("${StringUtils.lowercaseFirst($ServiceName)}")
public class ${ServiceImplName} #if(${superServiceImplClassPackage}) extends ${superServiceImplClass}<${EntityName}, ${entity}> #end implements ${ServiceName} {
	
}

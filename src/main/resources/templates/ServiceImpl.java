package ${packageServiceImpl};

import ${packageEntity};
import ${packageMapper};
import ${package.Service}.${table.serviceName};
import ${superServiceImplClassPackage};
import org.springframework.stereotype.Service;

/**
 * $!{table.comment} 服务实现类
 * @author ${author}
 * @since ${date}
 */
@Service("${ServiceName}")
public class ${ServiceImplName} extends ${superServiceImplClass}<${EntityName}, ${entity}> implements ${ServiceName} {
	
}

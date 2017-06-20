package ${packageServiceImpl};

import ${packageEntity}.${EntityName};
import ${packageMapper}.${MapperName};
import ${packageService}.${ServiceName};
#if(${superServiceImplClass})
import #if($StringUtils.indexOf("$superServiceImplClass",'.')==-1)${packageServiceImpl}.#end${superServiceImplClass};
#end
import org.springframework.stereotype.Service;

/**
 * $!{table.comment} 服务实现类
 * @author ${author}
 * @since ${date}
 */
@Service("${StringUtils.lowercaseFirst($ServiceName)}")
public class ${ServiceImplName} #if(${superServiceImplClass}) extends ${StringUtils.getClassName(${superServiceImplClass})} #end implements ${ServiceName} {
	
}

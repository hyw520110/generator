package ${mapperPackage};

import org.springframework.stereotype.Repository;

import ${entityPackage}.${entityName};
#if(${superMapperClass})
import #if($StringUtils.indexOf("$superMapperClass",'.')==-1)${mapperPackage}.#end${superMapperClass};
#end

import org.springframework.stereotype.Repository;

/**
 * <p>
  * $!{table.comment} Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since ${date}
 */
//TODO 泛型 联合主键处理<${className}>
@Repository
public interface ${className} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})} #end{

}
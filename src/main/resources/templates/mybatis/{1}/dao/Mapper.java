package ${packageMapper};

import org.springframework.stereotype.Repository;

import ${packageEntity}.${EntityName};
#if(${superMapperClass})
import #if($StringUtils.indexOf("$superMapperClass",'.')==-1)${packageMapper}.#end${superMapperClass};
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
//TODO 泛型 联合主键处理<${EntityName}>
@Repository
public interface ${MapperName} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})} #end{

}
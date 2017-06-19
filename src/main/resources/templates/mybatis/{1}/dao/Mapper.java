package ${packageMapper};

import ${packageEntity}.${EntityName};
#if(${superMapperClass})
import ${superMapperClass};
#end
/**
 * <p>
  * $!{table.comment} Mapper 接口
 * </p>
 *
 * @author ${author}
 * @since ${date}
 */
public interface ${MapperName} #if(${superMapperClass}) extends ${StringUtils.getClassName(${superMapperClass})}<${EntityName}> #end{

}
package ${servicePackage};

import ${entityPackage}.${entityName};
#if(${superServiceClass})
import #if($StringUtils.indexOf("$superServiceClass",'.')==-1)${servicePackage}.#end${superServiceClass};
#end

#parse('/templates/comments/comment.vm')
public interface ${className} #if(${superServiceClass}) extends ${StringUtils.getClassName(${superServiceClass})}<${StringUtils.capitalFirst("$entityName")}#if("plus"!="$mapperType"),${table.getPrimaryKeyClass()}#end> #end{
}

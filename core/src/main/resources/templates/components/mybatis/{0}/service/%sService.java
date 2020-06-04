package ${servicePackage};

import ${entityPackage}.${entityName};
#if(${superServiceClass})
import #if($StringUtils.indexOf("$superServiceClass",'.')==-1)${servicePackage}.#end${superServiceClass};
#end

#parse('/templates/comments/comment.vm')
public interface ${className} #if(${superServiceClass}) extends ${StringUtils.getClassName(${superServiceClass})}<${StringUtils.capitalFirst("$entityName")}> #end{
#if("plus"!="$mapperType")
	public ${StringUtils.capitalFirst("$entityName")} findById(#foreach($field in ${table.primarykeyFields})${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
	
	public Integer deleteById(#foreach($field in ${table.primarykeyFields})${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
#end
}

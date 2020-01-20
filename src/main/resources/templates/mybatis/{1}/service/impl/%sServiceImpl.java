package ${implPackage};

import ${entityPackage}.${entityName};
import ${mapperPackage}.${mapperName};
import ${servicePackage}.${serviceName};
#if(${superServiceImplClass})
import #if($StringUtils.indexOf("$superServiceImplClass",'.')==-1)${implPackage}.#end${superServiceImplClass};
#end
import org.springframework.beans.factory.annotation.Autowired;

#parse('/templates/commons/comment.vm')
@org.springframework.stereotype.Service
#if($!{DUBBO})
@org.apache.dubbo.config.annotation.Service
#end
public class ${className} #if(${superServiceImplClass}) extends ${StringUtils.getClassName(${superServiceImplClass})}<#if("plus"=="$mapperType")${StringUtils.capitalFirst("$entityName")}Mapper,#end${StringUtils.capitalFirst("$entityName")}> #end implements ${serviceName} {
#if("plus"!="$mapperType")
#set($sName=${StringUtils.lowercaseFirst($mapperName)})

	@Autowired
	private ${mapperName} ${sName};
	
	public ${StringUtils.capitalFirst("$entityName")} findById(#foreach($field in ${table.primarykeyFields})${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end){
		return ${sName}.findById(#foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
	}

	public Integer deleteById(#foreach($field in ${table.primarykeyFields})${field.fieldType.type} ${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end){
		return ${sName}.deleteById(#foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
	}
#end
}

package ${controllerPackage};

import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
#if($!{springboot_version})
import org.springframework.web.bind.annotation.RestController;
#else
import org.springframework.stereotype.Controller;
#end
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
#if(${superControllerClass})
import #if(${StringUtils.indexOf("$superControllerClass", '.')}==-1)${controllerPackage}.#end${superControllerClass};
#end
import ${entityPackage}.${entityName};
import ${servicePackage}.${serviceName};
#foreach($pkg in ${table.importPackages})
import ${pkg};
#end


#parse('/templates/commons/comment.vm')

#if($!{springboot_version})
@RestController
#else
@Controller
#end
@RequestMapping("/${table.beanName}")
public class ${className} #if(${superControllerClass})extends ${superControllerClass}#end {
#set($sName=${StringUtils.lowercaseFirst($serviceName)})
    
    @Autowired
    private ${serviceName} ${sName};
    
    @RequestMapping(value="/view", method = RequestMethod.GET)
    public ${StringUtils.capitalFirst("$entityName")} view(#foreach($field in ${table.primarykeyFields})@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.count!=${table.primarykeyFields.size()}),#end #end){
         return ${sName}.findById(#foreach($field in ${table.primarykeyFields})${field.propertyName} #if($foreach.count!=${table.primarykeyFields.size()}),#end #end);
    }
}

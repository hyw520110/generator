package ${controllerPackage};

import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
#if($!{springboot_version})
import org.springframework.web.bind.annotation.RestController;
#else
import org.springframework.stereotype.Controller;
#end
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
#if(${superControllerClass})
import ${superControllerClass};
#end
import ${entityPackage}.${entityName};
import ${servicePackage}.${serviceName};

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
    
    @RequestMapping("/list")
    public #if($!{resp_data_type_json})List<${entityName}>#else String #end list(HttpServletRequest req,HttpServletResponse resq, @RequestParam Map<String, Object> map , Model model){
        List<${entityName}> list=${sName}.findAll(map);
#if($!{resp_data_type_json})
		return list;
#else	
		model.addAttribute("list",list);
		return "${table.beanName}/list";
#end
    }
}

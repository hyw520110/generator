package ${controllerPackage};

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
#if(${superControllerClass})
import ${superControllerClass};
#end
import ${entityPackage}.${entityName};
import ${servicePackage}.${serviceName};

/**
 * $!{table.comment} 前端控制器
 * @author ${author}
 * @since ${date}
 */
@Controller
@RequestMapping("/${table.beanName}")
public class ${className} #if(${superControllerClass})extends ${superControllerClass}#end {
#set($sName=${StringUtils.lowercaseFirst($serviceName)})
    
    @Autowired
    private ${serviceName} ${sName};
    
    @RequestMapping("/list")
    public String list(HttpServletRequest req,HttpServletResponse resq, @RequestParam Map<String, Object> map , Model model){
        List<${className}> list=${sName}.findAll(map);
        model.addAttribute("list",list);
        return "${table.beanName}/list";
    }
}

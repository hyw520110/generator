package ${controllerPackage};

import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
#if($!{springboot_version})
import org.springframework.web.bind.annotation.RestController;
#else
import org.springframework.stereotype.Controller;
#end
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
#set($eName=${StringUtils.capitalFirst("$entityName")})

    @Autowired
    private ${serviceName} ${sName};
    
    @RequestMapping(value="/view/#foreach($field in ${table.primarykeyFields}){${field.propertyName}}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end", method = RequestMethod.GET)
    public #if($!{resp_data_type_json})${StringUtils.capitalFirst("$entityName")}#else ModelAndView #end view(#foreach($field in ${table.primarykeyFields})@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.count!=${table.primarykeyFields.size()}),#end #end){
        ${StringUtils.capitalFirst("$entityName")} bean= ${sName}.findById(#foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end);
        return #if($!{resp_data_type_json})bean#else new ModelAndView("${table.beanName}/view","bean",bean)#end ;
    }
    
    @RequestMapping(value="/list")
    public #if($!{resp_data_type_json})List<$eName>#else ModelAndView #end list(HttpServletRequest req,@RequestParam Map<String, Object> map ,@RequestParam(required = false, defaultValue = "1") int pageNo,@RequestParam(required = false, defaultValue = "10") int pageRows, Model model){
        PageHelper.startPage(pageNo, pageRows);
        List<$eName> list=${sName}.findAll(map);
#if($!{resp_data_type_json})
		return list;
#else
        model.addAttribute("list",list);
        return new ModelAndView("${table.beanName}/list","page",new PageInfo<$eName>(list));
#end
    }
    
    @RequestMapping(value="/add", method = RequestMethod.POST)
    public ModelAndView save(@RequestBody $eName entity){
        return new ModelAndView("redirect:${table.beanName}/list","flag",${sName}.save(entity));
    }
    
    @RequestMapping(value="/update", method = RequestMethod.POST)
    public ModelAndView update(@RequestBody $eName entity){
        return new ModelAndView("redirect:${table.beanName}/list","flag",${sName}.update(entity)>0);
    }
    
    @RequestMapping(value="/del/#foreach($field in ${table.primarykeyFields}){${field.propertyName}}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end", method = RequestMethod.POST)
    public ModelAndView delete(#foreach($field in ${table.primarykeyFields})@PathVariable(value = "${field.propertyName}") final ${field.fieldType.type} ${field.propertyName} #if($foreach.count!=${table.primarykeyFields.size()}),#end#end){
        return new ModelAndView("redirect:${table.beanName}/list","flag",${sName}.deleteById(#foreach($field in ${table.primarykeyFields})${field.propertyName}#if($foreach.count!=${table.primarykeyFields.size()}),#end#end)>0);
    }
}

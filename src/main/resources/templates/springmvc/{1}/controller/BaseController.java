#if(${StringUtils.indexOf("$superControllerClass", '.')}==-1)
package ${controllerPackage};

import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
#if($!{springboot_version})
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
#end
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ${entityPackage}.${entityName};
import ${servicePackage}.BaseService;

#parse('/templates/commons/comment.vm')
public class BaseController<T> {
    
    @Autowired
    private BaseService baseService;
    
    @RequestMapping(value="/list",method = RequestMethod.GET)
    public #if($!{resp_data_type_json})List<${entityName}>#else String #end list(HttpServletRequest req,HttpServletResponse resq, @RequestParam Map<String, Object> map , Model model){
        List<${entityName}> list=baseService.findAll(map);
#if($!{resp_data_type_json})
		return list;
#else	
		model.addAttribute("list",list);
		return "${table.beanName}/list";
#end
    }
    
    @RequestMapping(value="/save", method = RequestMethod.POST)
    public String save(@RequestBody final T entity){
    	 JSONObject json = new JSONObject();
         json.put("flag", baseService.save(entity));
         return json.toString();
    }
    
    @RequestMapping(value="/update", method = RequestMethod.POST)
    public String update(@RequestBody final T entity){
    	 JSONObject json = new JSONObject();
         json.put("flag", baseService.update(entity)>0);
         return json.toString();
    }
    
}
#end
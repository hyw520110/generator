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
    
    
}
#end
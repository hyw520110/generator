#if($!{VUE})
package ${controllerPackage};

import java.util.List;
import java.util.Map;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.web.servlet.ModelAndView;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
#if(${superControllerClass})
import #if(${StringUtils.indexOf("$superControllerClass", '.')}==-1)${controllerPackage}.commons.#end${superControllerClass};
#end
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
#if($!{resp_data_type_json})import ${voPackage}.Result;#end

#parse('/templates/commons/comment.vm')

#if($!{springboot_version})
@RestController("/auth")
#else
@Controller("/auth")
#end
public class LoginController #if(${superControllerClass})extends ${superControllerClass}#end {

	@PostMapping("/login")
	public String login(HttpServletRequest req) {
		System.out.println(req.getParameterMap());
		return "true";
	}
}
#end
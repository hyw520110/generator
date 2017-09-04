package ${rootPackage}.${projectName}.${moduleName}.controller.commons;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller  
public class ErrorController implements org.springframework.boot.autoconfigure.web.ErrorController {
	private static final String ERROR_PATH = "/error";

	@RequestMapping("/")
	public String index() {
		return "/index";
	}
	
	@RequestMapping(value = ERROR_PATH)
	public String handleError() {
		return "404";
	}

	@Override
	public String getErrorPath() {
		return ERROR_PATH;
	}
}

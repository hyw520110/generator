package ${commonsPackage!};

import ${dtoPackage!}.StatusCode;
import ${dtoPackage!}.Result;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自定义错误控制器，处理 /error 路径并返回 JSON 响应
 */
@RestController
public class ErrorController {
	private static final String ERROR_PATH = "/error";
	
	@RequestMapping(value = ERROR_PATH)
	public Result<?> handleError(HttpServletRequest request) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		
		if (status != null) {
			int statusCode = Integer.parseInt(status.toString());
			
			if (statusCode == HttpStatus.NOT_FOUND.value()) {
				return Result.error(StatusCode.DATA_NOT_EXIST_ERROR.getCode(), StatusCode.DATA_NOT_EXIST_ERROR.getDesc());
			} else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
				return Result.error(StatusCode.UNAUTH_ERROR.getCode(), StatusCode.UNAUTH_ERROR.getDesc());
			} else if (statusCode == HttpStatus.FORBIDDEN.value()) {
				return Result.error(StatusCode.SYSTEM_AUTH_ERROR.getCode(), StatusCode.SYSTEM_AUTH_ERROR.getDesc());
			}
		}
		
		return Result.error(StatusCode.SYSTEM_ERROR.getCode(), StatusCode.SYSTEM_ERROR.getDesc());
	}
}
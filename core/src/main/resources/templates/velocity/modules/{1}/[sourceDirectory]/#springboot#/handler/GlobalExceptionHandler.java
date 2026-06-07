package ${rootPackage}.${projectName}.${moduleName}.handler;

import ${validationPackage}.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

#if($global.modules && $global.modules.size() > 1)
import ${api_dtoPackage}.Result;
import ${api_dtoPackage}.StatusCode;
#else
import ${dtoPackage}.Result;
import ${dtoPackage}.StatusCode;
#end

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse(StatusCode.PARAM_ERROR.getDesc());
		return Result.error(StatusCode.PARAM_ERROR.getCode(), message);
	}

	@ExceptionHandler(BindException.class)
	public Result<?> handleBind(BindException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse(StatusCode.PARAM_ERROR.getDesc());
		return Result.error(StatusCode.PARAM_ERROR.getCode(), message);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public Result<?> handleConstraintViolation(ConstraintViolationException exception) {
		return Result.error(StatusCode.PARAM_ERROR.getCode(), exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public Result<?> handleException(Exception exception) {
		log.error("Unhandled application exception", exception);
		return Result.error(StatusCode.SYSTEM_ERROR.getCode(), StatusCode.SYSTEM_ERROR.getDesc());
	}
}

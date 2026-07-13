package ${rootPackage!}.${projectName!}.${moduleName!}.handler;

import ${validationPackage}.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
<#if global.modules?? && global.modules?size gt 1>
import ${api_dtoPackage!}.Result;
import ${api_dtoPackage!}.StatusCode;
<#else>
import ${dtoPackage!}.Result;
import ${dtoPackage!}.StatusCode;
</#if>

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(StatusCode.PARAM_ERROR.getDesc());
        return Result.error(StatusCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(StatusCode.PARAM_ERROR.getDesc());
        return Result.error(StatusCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException ex) {
        return Result.error(StatusCode.PARAM_ERROR.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        log.error("Unhandled application exception", ex);
        return Result.error(StatusCode.SYSTEM_ERROR.getCode(), StatusCode.SYSTEM_ERROR.getDesc());
    }
}

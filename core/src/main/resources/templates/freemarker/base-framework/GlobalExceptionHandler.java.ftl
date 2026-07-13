package ${rootPackage}.common.handler;

import ${rootPackage}.core.exception.BusinessException;
import ${rootPackage}.core.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理，统一将应用层、校验层和系统层异常收敛为标准的 Result JSON 响应。
 * 遵循项目 AGENTS.md 规范，禁止把异常直接抛给客户端导致内部结构泄露。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常拦截: code={}, message={}", e.getCode(), e.getMessage());
        return Result.failure(e.getCode(), e.getMessage());
    }

    /**
     * 捕获参数校验异常 (@RequestBody 等实体类校验)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", msg);
        // 此处假设 ResultCode.BAD_REQUEST 或者类似枚举存在，具体以后端 core 代码为准
        // 如果不存在，用通用的失败码
        return Result.failure(400, "参数校验失败: " + msg);
    }

    /**
     * 捕获参数绑定异常 (URL参数校验)
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", msg);
        return Result.failure(400, "参数校验失败: " + msg);
    }

    /**
     * 捕获散装参数校验异常 (@RequestParam, @PathVariable 等)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("散装参数校验失败: {}", msg);
        return Result.failure(400, "参数校验失败: " + msg);
    }

    /**
     * 兜底：处理所有未预料到的系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统发生未知异常", e);
        return Result.failure(500, "系统繁忙，请稍后重试");
    }
}

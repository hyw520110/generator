package org.hyw.tools.generator.exception;

/**
 * 代码生成器异常类
 * 
 * @author: heyiwu
 * @version: 1.0 Create at: 2026-03-19
 */
public class GeneratorException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private ErrorCode errorCode;

    public enum ErrorCode {
        DATABASE_METADATA_READ_FAILED("数据库元数据读取失败"),
        DATABASE_QUERY_FAILED("数据库查询失败"),
        TEMPLATE_RENDER_FAILED("模板渲染失败"),
        FILE_WRITE_FAILED("文件写入失败"),
        CONFIGURATION_ERROR("配置错误"),
        CONNECTION_ERROR("连接错误"),
        VALIDATION_ERROR("验证错误");
        
        private final String description;
        
        ErrorCode(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    public GeneratorException(String message) {
        super(message);
    }

    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneratorException(ErrorCode errorCode, Throwable cause, String context) {
        super(errorCode.getDescription() + ": " + context, cause);
        this.errorCode = errorCode;
    }

    public GeneratorException(Throwable cause) {
        super(cause);
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
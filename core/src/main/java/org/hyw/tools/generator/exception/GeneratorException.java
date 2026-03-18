package org.hyw.tools.generator.exception;

import java.text.MessageFormat;

import lombok.Getter;

/**
 * 代码生成器异常基类
 * <p>
 * 提供错误码和错误参数支持
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Getter
public class GeneratorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 错误参数
     */
    private final Object[] errorArgs;

    /**
     * 构造异常
     *
     * @param message 错误消息
     */
    public GeneratorException(String message) {
        super(message);
        this.errorCode = ErrorCode.GENERATOR_ERROR.getCode();
        this.errorArgs = null;
    }

    /**
     * 构造异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.GENERATOR_ERROR.getCode();
        this.errorArgs = null;
    }

    /**
     * 构造异常
     *
     * @param errorCode 错误码
     * @param message   错误消息（支持占位符）
     * @param args      错误参数
     */
    public GeneratorException(String errorCode, String message, Object... args) {
        super(formatMessage(message, args));
        this.errorCode = errorCode;
        this.errorArgs = args;
    }

    /**
     * 构造异常
     *
     * @param errorCode 错误码
     * @param message   错误消息（支持占位符）
     * @param cause     原因
     * @param args      错误参数
     */
    public GeneratorException(String errorCode, String message, Throwable cause, Object... args) {
        super(formatMessage(message, args), cause);
        this.errorCode = errorCode;
        this.errorArgs = args;
    }

    /**
     * 构造异常（使用错误码枚举）
     *
     * @param errorCode 错误码枚举
     * @param args      错误参数
     */
    public GeneratorException(ErrorCode errorCode, Object... args) {
        super(formatMessage(errorCode.getMessage(), args));
        this.errorCode = errorCode.getCode();
        this.errorArgs = args;
    }

    /**
     * 构造异常（使用错误码枚举）
     *
     * @param errorCode 错误码枚举
     * @param cause     原因
     * @param args      错误参数
     */
    public GeneratorException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(formatMessage(errorCode.getMessage(), args), cause);
        this.errorCode = errorCode.getCode();
        this.errorArgs = args;
    }

    /**
     * 格式化消息
     */
    private static String formatMessage(String message, Object[] args) {
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return MessageFormat.format(message, args);
        } catch (Exception e) {
            return message;
        }
    }

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        // 通用错误 (1xxx)
        GENERATOR_ERROR("GEN_1000", "生成器错误：{0}"),
        CONFIG_NOT_FOUND("GEN_1001", "配置文件不存在：{0}"),
        CONFIG_INVALID("GEN_1002", "配置无效：{0}"),
        
        // 模板错误 (2xxx)
        TEMPLATE_NOT_FOUND("TPL_2000", "模板文件不存在：{0}"),
        TEMPLATE_RENDER_FAILED("TPL_2001", "模板渲染失败：{0}"),
        TEMPLATE_ENGINE_INIT_FAILED("TPL_2002", "模板引擎初始化失败：{0}"),
        
        // 数据库错误 (3xxx)
        DATABASE_CONNECTION_FAILED("DB_3000", "数据库连接失败：{0}"),
        DATABASE_QUERY_FAILED("DB_3001", "数据库查询失败：{0}"),
        DATABASE_METADATA_READ_FAILED("DB_3002", "读取数据库元数据失败：{0}"),
        
        // 文件操作错误 (4xxx)
        FILE_WRITE_FAILED("FILE_4000", "文件写入失败：{0}"),
        FILE_READ_FAILED("FILE_4001", "文件读取失败：{0}"),
        FILE_DELETE_FAILED("FILE_4002", "文件删除失败：{0}"),
        
        // 参数错误 (5xxx)
        PARAMETER_INVALID("PARAM_5000", "参数无效：{0}"),
        PARAMETER_MISSING("PARAM_5001", "缺少必要参数：{0}");

        private final String code;
        private final String message;

        ErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}

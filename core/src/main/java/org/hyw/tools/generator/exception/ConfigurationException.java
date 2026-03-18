package org.hyw.tools.generator.exception;

/**
 * 配置异常
 * <p>
 * 当配置无效或缺失时抛出
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class ConfigurationException extends GeneratorException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造异常
     *
     * @param message 错误消息
     */
    public ConfigurationException(String message) {
        super("CONFIG_ERROR", message);
    }

    /**
     * 构造异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public ConfigurationException(String message, Throwable cause) {
        super("CONFIG_ERROR", message, cause);
    }

    /**
     * 构造异常
     *
     * @param message 错误消息（支持占位符）
     * @param args    错误参数
     */
    public ConfigurationException(String message, Object... args) {
        super("CONFIG_ERROR", message, args);
    }
}

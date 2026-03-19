package org.hyw.tools.generator.exception;

/**
 * 数据库异常
 * <p>
 * 当数据库操作失败时抛出
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class DatabaseException extends GeneratorException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造异常
     *
     * @param message 错误消息
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * 构造异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造异常
     *
     * @param message 错误消息（支持占位符）
     * @param args    错误参数
     */
    public DatabaseException(String message, Object... args) {
        super(message);
    }
}

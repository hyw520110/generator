package org.hyw.tools.generator.exception;

import java.io.File;

/**
 * 文件操作异常
 * <p>
 * 当文件读写、删除等操作失败时抛出
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class FileOperationException extends GeneratorException {

    private static final long serialVersionUID = 1L;

    /**
     * 相关文件
     */
    private final File file;

    /**
     * 构造异常
     *
     * @param file    相关文件
     * @param message 错误消息
     */
    public FileOperationException(File file, String message) {
        super("FILE_ERROR", message);
        this.file = file;
    }

    /**
     * 构造异常
     *
     * @param file    相关文件
     * @param message 错误消息
     * @param cause   原因
     */
    public FileOperationException(File file, String message, Throwable cause) {
        super("FILE_ERROR", message, cause);
        this.file = file;
    }

    /**
     * 构造异常
     *
     * @param file    相关文件
     * @param message 错误消息（支持占位符）
     * @param args    错误参数
     */
    public FileOperationException(File file, String message, Object... args) {
        super("FILE_ERROR", message, args);
        this.file = file;
    }

    /**
     * 获取相关文件
     *
     * @return 相关文件
     */
    public File getFile() {
        return file;
    }
}

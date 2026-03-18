package org.hyw.tools.generator.exception;

/**
 * 模板渲染异常
 * <p>
 * 当模板渲染失败时抛出
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class TemplateRenderException extends GeneratorException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造异常
     *
     * @param message 错误消息
     */
    public TemplateRenderException(String message) {
        super("TEMPLATE_RENDER_ERROR", message);
    }

    /**
     * 构造异常
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public TemplateRenderException(String message, Throwable cause) {
        super("TEMPLATE_RENDER_ERROR", message, cause);
    }

    /**
     * 构造异常
     *
     * @param message 错误消息（支持占位符）
     * @param args    错误参数
     */
    public TemplateRenderException(String message, Object... args) {
        super("TEMPLATE_RENDER_ERROR", message, args);
    }
}

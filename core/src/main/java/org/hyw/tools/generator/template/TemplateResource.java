package org.hyw.tools.generator.template;

import java.io.IOException;
import java.io.InputStream;

/**
 * 模板资源接口 (支持按需加载与流式处理)
 */
public interface TemplateResource {

    /**
     * 获取模板路径 (用于生成目标路径)
     */
    String getPath();

    /**
     * 获取模板名称
     */
    String getName();

    /**
     * 获取模板内容 (文本)
     */
    String getContent() throws IOException;
    
    /**
     * 是否为二进制资源
     */
    boolean isBinary();
    
    /**
     * 获取二进制内容
     */
    byte[] getBytes() throws IOException;

    /**
     * 打开输入流 (用于流式拷贝或渲染)
     */
    InputStream openStream() throws IOException;
}

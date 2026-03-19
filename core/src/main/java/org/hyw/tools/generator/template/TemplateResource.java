package org.hyw.tools.generator.template;

import java.io.IOException;

/**
 * 模板资源接口 (支持按需加载)
 * <p>
 * 用于封装模板文件，支持延迟读取内容，优化内存占用。
 * </p>
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
     * @return 模板内容
     * @throws IOException 读取失败时抛出
     */
    String getContent() throws IOException;
    
    /**
     * 是否为二进制资源
     * @return true 如果是二进制文件
     */
    boolean isBinary();
    
    /**
     * 获取二进制内容
     * @return 字节数组
     * @throws IOException 读取失败时抛出
     */
    byte[] getBytes() throws IOException;
}

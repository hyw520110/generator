package org.hyw.tools.generator.writer;

import java.io.File;
import java.io.IOException;

import org.hyw.tools.generator.exception.FileOperationException;

/**
 * 文件写入器接口
 * 
 * 定义文件操作的统一接口
 * 支持写入文件、检查文件存在、删除文件等操作
 * 
 * @author heyiwu
 * @version 2.0
 */
public interface FileWriter {
    
    /**
     * 写入文件
     * 
     * @param path 文件路径（相对于输出目录）
     * @param content 文件内容
     * @throws FileOperationException 写入失败时抛出
     */
    void write(String path, String content) throws FileOperationException;
    
    /**
     * 写入文件（指定是否覆盖）
     * 
     * @param path 文件路径
     * @param content 文件内容
     * @param override 是否覆盖已存在的文件
     * @throws FileOperationException 写入失败时抛出
     */
    void write(String path, String content, boolean override) throws FileOperationException;
    
    /**
     * 检查文件是否存在
     * 
     * @param path 文件路径
     * @return 是否存在
     */
    boolean exists(String path);
    
    /**
     * 删除文件
     * 
     * @param path 文件路径
     * @throws FileOperationException 删除失败时抛出
     */
    void delete(String path) throws FileOperationException;
    
    /**
     * 删除目录
     * 
     * @param path 目录路径
     * @throws FileOperationException 删除失败时抛出
     */
    void deleteDirectory(String path) throws FileOperationException;
    
    /**
     * 创建目录
     * 
     * @param path 目录路径
     * @throws FileOperationException 创建失败时抛出
     */
    void mkdirs(String path) throws FileOperationException;
    
    /**
     * 复制文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @throws FileOperationException 复制失败时抛出
     */
    void copy(String sourcePath, String targetPath) throws FileOperationException;
    
    /**
     * 获取输出目录
     * 
     * @return 输出目录
     */
    File getOutputDirectory();
    
    /**
     * 设置输出目录
     * 
     * @param outputDir 输出目录
     */
    void setOutputDirectory(File outputDir);
    
    /**
     * 设置是否覆盖已存在文件
     * 
     * @param override 是否覆盖
     */
    void setOverride(boolean override);
}
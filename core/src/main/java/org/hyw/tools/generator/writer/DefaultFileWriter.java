package org.hyw.tools.generator.writer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.hyw.tools.generator.exception.FileOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认文件写入器实现
 * <p>
 * 主要改进:
 * 1. 使用 Charset 代替 String 编码名
 * 2. 支持 BOM 头处理 (针对 Windows GBK 编码)
 * 3. 更完善的异常处理
 * </p>
 *
 * @author heyiwu
 * @version: 2.0
 */
public class DefaultFileWriter implements FileWriter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultFileWriter.class);

    /**
     * UTF-8 BOM 头字节
     */
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    /**
     * 输出目录
     */
    private File outputDirectory;

    /**
     * 是否覆盖已有文件
     */
    private boolean override = false;

    /**
     * 文件编码
     */
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * 是否添加 BOM 头
     */
    private boolean addBom = false;

    /**
     * 使用默认输出目录创建写入器
     */
    public DefaultFileWriter() {
        this(new File("."));
    }

    /**
     * 使用指定输出目录创建写入器
     *
     * @param outputDirectory 输出目录
     */
    public DefaultFileWriter(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void write(String path, String content) throws FileOperationException {
        write(path, content, override);
    }

    @Override
    public void write(String path, String content, boolean override) throws FileOperationException {
        if (path == null || path.trim().isEmpty()) {
            throw new FileOperationException(new File(path), "文件路径不能为空");
        }

        if (content == null) {
            throw new FileOperationException(new File(path), "文件内容不能为空");
        }

        File file = new File(outputDirectory, path);

        // 检查是否覆盖
        if (file.exists() && !override) {
            logger.debug("文件已存在，跳过写入：{}", file);
            return;
        }

        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    logger.debug("创建目录：{}", parentDir);
                }
            }

            // 写入文件 (带 BOM 头处理)
            writeFile(file, content);
            logger.info("{} 文件：{}", file.exists() ? "覆盖" : "生成", file);

        } catch (IOException e) {
            logger.error("写入文件失败：{}", file, e);
            throw new FileOperationException(file, "写入文件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 写入文件 (带 BOM 头处理)
     */
    private void writeFile(File file, String content) throws IOException {
        byte[] bytes = content.getBytes(charset);

        // 如果需要添加 BOM 头
        if (addBom && needBOM(charset)) {
            byte[] bytesWithBom = new byte[UTF8_BOM.length + bytes.length];
            System.arraycopy(UTF8_BOM, 0, bytesWithBom, 0, UTF8_BOM.length);
            System.arraycopy(bytes, 0, bytesWithBom, UTF8_BOM.length, bytes.length);
            FileUtils.writeByteArrayToFile(file, bytesWithBom);
        } else {
            FileUtils.writeByteArrayToFile(file, bytes);
        }
    }

    /**
     * 判断是否需要添加 BOM 头
     */
    private boolean needBOM(Charset charset) {
        // 只有 UTF-8 在 Windows 环境下可能需要 BOM 头
        return StandardCharsets.UTF_8.equals(charset) && addBom;
    }

    @Override
    public boolean exists(String path) {
        if (path == null) {
            return false;
        }
        File file = new File(outputDirectory, path);
        return file.exists();
    }

    @Override
    public void delete(String path) throws FileOperationException {
        if (path == null) {
            throw new FileOperationException(new File(path), "文件路径不能为空");
        }

        File file = new File(outputDirectory, path);
        if (!file.exists()) {
            logger.debug("文件不存在，跳过删除：{}", file);
            return;
        }

        try {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("删除文件：{}", file);
            } else {
                throw new FileOperationException(file, "删除文件失败");
            }
        } catch (SecurityException e) {
            logger.error("删除文件失败：{}", file, e);
            throw new FileOperationException(file, "删除文件失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void deleteDirectory(String path) throws FileOperationException {
        if (path == null) {
            throw new FileOperationException(new File(path), "目录路径不能为空");
        }

        File dir = new File(outputDirectory, path);
        if (!dir.exists()) {
            logger.debug("目录不存在，跳过删除：{}", dir);
            return;
        }

        if (!dir.isDirectory()) {
            throw new FileOperationException(dir, "路径不是目录");
        }

        try {
            logger.info("删除目录：{}", dir);
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            logger.error("删除目录失败：{}", dir, e);
            throw new FileOperationException(dir, "删除目录失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void mkdirs(String path) throws FileOperationException {
        if (path == null || path.trim().isEmpty()) {
            throw new FileOperationException(new File(path), "目录路径不能为空");
        }

        File dir = new File(outputDirectory, path);
        if (dir.exists()) {
            logger.debug("目录已存在：{}", dir);
            return;
        }

        boolean created = dir.mkdirs();
        if (created) {
            logger.debug("创建目录：{}", dir);
        }
    }

    @Override
    public void copy(String sourcePath, String targetPath) throws FileOperationException {
        if (sourcePath == null || targetPath == null) {
            throw new FileOperationException(new File(targetPath), "源路径和目标路径不能为空");
        }

        File source = new File(sourcePath);
        File target = new File(outputDirectory, targetPath);

        if (!source.exists()) {
            throw new FileOperationException(source, "源文件不存在");
        }

        try {
            // 确保目标目录存在
            File targetDir = target.getParentFile();
            if (targetDir != null && !targetDir.exists()) {
                targetDir.mkdirs();
            }

            FileUtils.copyFile(source, target);
            logger.info("复制文件：{} -> {}", source, target);

        } catch (IOException e) {
            logger.error("复制文件失败：{} -> {}", source, target, e);
            throw new FileOperationException(target, "复制文件失败：" + e.getMessage(), e);
        }
    }

    @Override
    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public void setOutputDirectory(File outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("输出目录不能为空");
        }
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void setOverride(boolean override) {
        this.override = override;
    }

    /**
     * 设置文件编码
     *
     * @param encoding 编码名称 (如：UTF-8、GBK)
     */
    public void setEncoding(String encoding) {
        if (encoding == null || encoding.trim().isEmpty()) {
            throw new IllegalArgumentException("编码不能为空");
        }
        this.charset = Charset.forName(encoding);
    }

    /**
     * 获取文件编码
     *
     * @return 编码名称
     */
    public String getEncoding() {
        return charset.name();
    }

    /**
     * 设置是否添加 BOM 头
     * <p>
     * 注意：只有 UTF-8 编码在 Windows 环境下可能需要 BOM 头
     * </p>
     *
     * @param addBom 是否添加 BOM 头
     */
    public void setAddBom(boolean addBom) {
        this.addBom = addBom;
    }

    /**
     * 是否添加 BOM 头
     *
     * @return 是否添加 BOM 头
     */
    public boolean isAddBom() {
        return addBom;
    }

    /**
     * 获取 Charset 对象
     *
     * @return Charset 对象
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * 设置 Charset 对象
     *
     * @param charset 字符集
     */
    public void setCharset(Charset charset) {
        if (charset == null) {
            throw new IllegalArgumentException("字符集不能为空");
        }
        this.charset = charset;
    }
}

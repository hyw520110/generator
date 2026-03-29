package org.hyw.tools.generator.conf;

import java.io.Serializable;

import org.hyw.tools.generator.enums.ProjectBuilder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目配置
 * <p>
 * 管理项目相关的配置（包名、模块、版本等）
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 父包名
     */
    @Builder.Default
    private String rootPackage = "com.hyw";

    /**
     * 模块信息
     */
    private String[] modules;

    /**
     * 开发人员
     */
    private String author;

    /**
     * 版权信息
     */
    @Builder.Default
    private String copyright = "";

    /**
     * 项目描述
     */
    private String description;

    /**
     * 工程构建工具
     */
    private ProjectBuilder projectBuilder;

    /**
     * 工程默认版本号
     */
    @Builder.Default
    private String version = "1.0.0";

    /**
     * JDK 版本
     */
    private String javaVersion;

    /**
     * 工程目录配置
     */
    @Builder.Default
    private DirectoryConfig directory = new DirectoryConfig();

    /**
     * 获取项目名（从输出目录获取）
     */
    public String getProjectName(String outputDir) {
        if (outputDir == null || outputDir.trim().isEmpty()) {
            return "";
        }
        int lastSlash = outputDir.lastIndexOf('/');
        if (lastSlash == -1) {
            lastSlash = outputDir.lastIndexOf('\\');
        }
        if (lastSlash == -1 || lastSlash >= outputDir.length() - 1) {
            return outputDir;
        }
        return outputDir.substring(lastSlash + 1);
    }

    /**
     * 目录配置
     */
    @Data
    @NoArgsConstructor
    public static class DirectoryConfig implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 主代码目录
         */
        private String sourceDirectory = "src/main/java";

        /**
         * 主资源目录
         */
        private String resourceDirectory = "src/main/resources";

        /**
         * 测试代码目录
         */
        private String testSourceDirectory = "src/test/java";

        /**
         * 测试资源目录
         */
        private String testResourceDirectory = "src/test/resources";
    }
}

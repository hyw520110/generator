package org.hyw.tools.generator.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.hyw.tools.generator.conf.GlobalConf;
import org.hyw.tools.generator.enums.Component;
import org.hyw.tools.generator.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

/**
 * 配置验证器
 * <p>
 * 负责验证全局配置的有效性
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class ConfigValidator {

    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);

    /**
     * 包名验证正则表达式
     */
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$");

    /**
     * 私有构造函数，防止实例化
     */
    private ConfigValidator() {
        // 工具类
    }

    /**
     * 验证配置
     *
     * @param config 全局配置
     * @throws ConfigurationException 配置验证失败时抛出
     */
    public static void validate(GlobalConf config) throws ConfigurationException {
        if (config == null) {
            throw new ConfigurationException("配置不能为空");
        }

        List<String> errors = new ArrayList<>();

        // 验证输出目录
        if (StringUtils.isBlank(config.getOutputDir())) {
            errors.add("输出目录不能为空");
        }

        // 验证根包名
        if (StringUtils.isBlank(config.getRootPackage())) {
            errors.add("根包名不能为空");
        } else if (!isValidPackageName(config.getRootPackage())) {
            errors.add("根包名格式不正确：" + config.getRootPackage());
        }

        // 验证模块配置
        if (config.getModules() == null || config.getModules().length == 0) {
            errors.add("模块配置不能为空");
        }

        // 验证组件配置
        if (config.getComponents() == null || config.getComponents().length == 0) {
            errors.add("组件配置不能为空");
        }

        // 验证 Java 版本
        if (StringUtils.isNotBlank(config.getJavaVersion())) {
            if (!isValidJavaVersion(config.getJavaVersion())) {
                errors.add("Java 版本格式不正确：" + config.getJavaVersion());
            }
        }

        // 验证版本号
        if (StringUtils.isNotBlank(config.getVersion())) {
            if (!isValidVersion(config.getVersion())) {
                errors.add("版本号格式不正确：" + config.getVersion());
            }
        }

        // 验证编码
        if (StringUtils.isNotBlank(config.getEncoding())) {
            if (!isValidEncoding(config.getEncoding())) {
                errors.add("不支持的编码格式：" + config.getEncoding());
            }
        }

        // 如果有错误，抛出异常
        if (!errors.isEmpty()) {
            String errorMessage = "配置验证失败：" + String.join(", ", errors);
            logger.error(errorMessage);
            throw new ConfigurationException(errorMessage);
        }

        logger.debug("配置验证通过");
    }

    /**
     * 验证配置（返回验证结果）
     *
     * @param config 全局配置
     * @return 验证结果
     */
    public static ValidationResult validateWithResult(GlobalConf config) {
        ValidationResult result = new ValidationResult();

        if (config == null) {
            result.addError("配置不能为空");
            return result;
        }

        // 验证输出目录
        if (StringUtils.isBlank(config.getOutputDir())) {
            result.addError("输出目录不能为空");
        }

        // 验证根包名
        if (StringUtils.isBlank(config.getRootPackage())) {
            result.addError("根包名不能为空");
        } else if (!isValidPackageName(config.getRootPackage())) {
            result.addError("根包名格式不正确：" + config.getRootPackage());
        }

        // 验证模块配置
        if (config.getModules() == null || config.getModules().length == 0) {
            result.addError("模块配置不能为空");
        }

        // 验证组件配置
        if (config.getComponents() == null || config.getComponents().length == 0) {
            result.addError("组件配置不能为空");
        }

        // 验证 Java 版本
        if (StringUtils.isNotBlank(config.getJavaVersion())) {
            if (!isValidJavaVersion(config.getJavaVersion())) {
                result.addError("Java 版本格式不正确：" + config.getJavaVersion());
            }
        }

        // 验证版本号
        if (StringUtils.isNotBlank(config.getVersion())) {
            if (!isValidVersion(config.getVersion())) {
                result.addError("版本号格式不正确：" + config.getVersion());
            }
        }

        // 验证编码
        if (StringUtils.isNotBlank(config.getEncoding())) {
            if (!isValidEncoding(config.getEncoding())) {
                result.addError("不支持的编码格式：" + config.getEncoding());
            }
        }

        return result;
    }

    /**
     * 验证包名格式
     *
     * @param packageName 包名
     * @return 是否有效
     */
    public static boolean isValidPackageName(String packageName) {
        return packageName != null && PACKAGE_PATTERN.matcher(packageName).matches();
    }

    /**
     * 验证 Java 版本
     *
     * @param version Java 版本号
     * @return 是否有效
     */
    public static boolean isValidJavaVersion(String version) {
        if (version == null) {
            return false;
        }
        // 支持格式：1.8, 8, 11, 17, 21 等
        return version.matches("^(1\\.)?[0-9]+$");
    }

    /**
     * 验证版本号格式（语义化版本）
     *
     * @param version 版本号
     * @return 是否有效
     */
    public static boolean isValidVersion(String version) {
        if (version == null) {
            return false;
        }
        // 支持格式：1.0.0, 1.0.1-SNAPSHOT, 2.3.4-RC1 等
        return version.matches("^[0-9]+\\.[0-9]+\\.[0-9]+(-[a-zA-Z0-9]+)?$");
    }

    /**
     * 验证编码格式
     *
     * @param encoding 编码名称
     * @return 是否有效
     */
    public static boolean isValidEncoding(String encoding) {
        if (encoding == null) {
            return false;
        }
        // 常见编码格式
        String[] validEncodings = {"UTF-8", "GBK", "GB2312", "ISO-8859-1", "US-ASCII"};
        for (String valid : validEncodings) {
            if (valid.equalsIgnoreCase(encoding)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证并构建配置（流式 API）
     *
     * @param config 全局配置
     * @return 验证通过返回配置对象
     * @throws ConfigurationException 验证失败抛出
     */
    public static GlobalConf validateAndBuild(GlobalConf config) throws ConfigurationException {
        validate(config);
        return config;
    }

    /**
     * 验证组件配置
     *
     * @param components 组件数组
     * @return 验证结果
     */
    public static ValidationResult validateComponents(Component[] components) {
        return org.hyw.tools.generator.enums.ComponentGroup.validate(components);
    }

    /**
     * 验证结果类
     */
    @Getter
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> infos = new ArrayList<>();

        /**
         * 添加错误
         */
        public void addError(String error) {
            errors.add(error);
        }

        /**
         * 添加警告
         */
        public void addWarn(String warning) {
            warnings.add(warning);
        }

        /**
         * 添加信息
         */
        public void addInfo(String info) {
            infos.add(info);
        }

        /**
         * 是否有错误
         */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        /**
         * 是否有警告
         */
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        /**
         * 是否有效 (无错误)
         */
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * 获取错误列表
         */
        public List<String> getErrors() {
            return errors;
        }

        /**
         * 获取警告列表
         */
        public List<String> getWarnings() {
            return warnings;
        }

        /**
         * 获取信息列表
         */
        public List<String> getInfos() {
            return infos;
        }

        /**
         * 获取错误消息
         */
        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return "";
            }
            return "验证失败：" + String.join(", ", errors);
        }
    }
}

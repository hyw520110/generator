package org.hyw.tools.generator.constants;

/**
 * 验证相关常量
 * <p>
 * 用于存储IP地址、端口号等验证相关的正则表达式和配置
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class ValidationConstants {

    /**
     * IP地址验证正则（支持域名和IPv4）
     * - 域名：example.com, example.cn 等
     * - IPv4: 192.168.1.1
     */
    public static final String[] IP_PATTERNS = {
        ".*?\\.(com|cn|net|org)",  // 域名
        "((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}"  // IPv4
    };

    /**
     * 端口号验证正则（1-65535）
     */
    public static final String[] PORT_PATTERNS = {
        "([0-9]|[1-9]\\d{1,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])"
    };

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY = 3;

    /**
     * 组件配置预设
     */
    public static final String[] COMPONENT_PRESETS = {
        "MYBATIS,SPRINGMVC,SPRINGBOOT,SPRINGCLOUD,ZOOKEEPER,SWAGGER2,DUBBO,REDIS,SHIRO,JWT,VUE",
        "MYBATIS,SPRINGMVC,SPRINGBOOT,SPRINGCLOUD,ZOOKEEPER,SWAGGER2,DUBBO,REDIS,SHIRO,JWT,VUE,SENTINEL,SKYWALKING"
    };

    private ValidationConstants() {
        // 防止实例化
    }
}
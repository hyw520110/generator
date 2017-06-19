package com.tzg.tools.generator.utils;

import java.io.File;

public class StringUtils extends org.apache.commons.lang.StringUtils {

    /**
     * 去掉下划线前缀
     * 
     * @param name
     * @return
     */
    public static String removePrefix(String name) {
        if (StringUtils.isEmpty(name)) {
            return "";
        }
        int idx = name.indexOf("_");
        if (idx == -1) {
            return name;
        }
        return name.substring(idx + 1);
    }

    /**
     * 去掉指定的前缀
     *
     * @param name
     * @param prefix
     * @return
     */
    public static String removePrefix(String name, String[] prefix) {
        if (StringUtils.isEmpty(name) || null == prefix || prefix.length == 0) {
            return name;
        }
        for (String s : prefix) {
            if (name.toLowerCase().matches("^" + s.toLowerCase() + ".*")) {
                // 判断是否有匹配的前缀，然后截取前缀
                // 删除前缀
                return name.substring(s.length());
            }
        }

        return name;
    }

    /**
     * 去掉下划线前缀且将后半部分转成驼峰格式
     *
     * @param name
     * @param tablePrefix
     * @param separators
     * @return
     */
    public static String removePrefixAndCamel(String name, String[] tablePrefix, char[] separators) {
        String s = removePrefix(name, tablePrefix);
        if (null == separators || separators.length == 0) {
            return s;
        }
        for (char c : separators) {
            if (StringUtils.contains(s, c)) {
                s = StringUtils.toCamelCase(s, c);
            }
        }
        return s;
    }

    /**
     * 实体首字母大写
     *
     * @param name
     *            待转换的字符串
     * @return 转换后的字符串
     */
    public static String capitalFirst(String name) {
        if (StringUtils.isNotEmpty(name)) {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return "";
    }

    public static String lowercaseFirst(String name) {
        if (StringUtils.isNotEmpty(name)) {
            return name.substring(0, 1).toLowerCase() + name.substring(1);
        }
        return "";
    }

    public static String toCamelCase(String s) {
        return toCamelCase(s, '_');
    }

    /**
     * 转驼峰命名,如：Tzg-web或tzg_web转TzgWeb
     * 
     * @author: heyiwu
     * @param s
     * @param separator
     *            分解字符
     * @return
     */
    public static String toCamelCase(String s, char separator) {
        return toCamelCase(s, separator, true);
    }

    /**
     * 转驼峰命名
     * 
     * @author: heyiwu
     * @param s
     * @param separator
     *            分解字符
     * @param firstUp
     *            首字母是否大写
     * @return
     */
    public static String toCamelCase(String s, char separator, boolean firstUp) {
        if (StringUtils.isBlank(s)) {
            return "";
        }
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c == separator) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(s.charAt(i)));
                }
            } else {
                if (i == 0 && firstUp) {
                    sb.append(Character.toUpperCase(s.charAt(i)));
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 表名称包含指定前缀
     * 
     * @param tabName
     *            表名称
     * @return
     */
    public static boolean startWithTablePrefix(String tabName, String[] names) {
        if (StringUtils.isBlank(tabName) || null == names || names.length == 0) {
            return false;
        }
        for (String s : names) {
            if (StringUtils.startsWith(tabName, s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为大写命名
     * 
     * @param s
     *            待判断字符串
     * @return
     */
    public static boolean isCapitalMode(String s) {
        return null != s && s.matches("^[0-9A-Z/_]+$");
    }

    /**
     * 包含大写字母
     * 
     * @param word
     *            待判断字符串
     * @return
     */
    public static boolean containsUpperCase(String word) {
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(String[] array, String name, boolean bDefault) {
        if (array == null || array.length == 0) {
            return bDefault;
        }
        for (String item : array) {
            if (item.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static String toPath(String... args) {
        if (null == args || args.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String s : args) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            builder.append(s.replaceAll("\\.", File.separator + File.separator) + File.separator);
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    public static String toPackage(String... args) {
        if (null == args || args.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String s : args) {
            builder.append(s + ".");
        }
        return builder.deleteCharAt(builder.length() - 1).toString();
    }

    public static String toPackage(String s) {
        return StringUtils.replace(s, File.separator, ".");
    }

    public static String getClassName(String className) {
        return StringUtils.substringAfterLast(className, ".");
    }
}

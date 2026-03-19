package org.hyw.tools.generator.template;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Velocity 模板工具类
 * <p>
 * 提供模板中可调用的静态工具方法
 * </p>
 *
 * @author heyiwu
 * @version 2.0
 */
public class TemplateUtils {

    private TemplateUtils() {
        // 工具类
    }

    /**
     * 导入包语句
     *
     * @param className 类名（全限定名）
     * @return import 语句
     */
    public static String importClass(String className) {
        return "import " + className + ";";
    }

    /**
     * 导入多个包
     *
     * @param classNames 类名列表
     * @return import 语句列表
     */
    public static String importClasses(List<String> classNames) {
        if (classNames == null || classNames.isEmpty()) {
            return "";
        }
        return classNames.stream()
            .map(TemplateUtils::importClass)
            .collect(Collectors.joining("\n"));
    }

    /**
     * 生成字段定义
     *
     * @param type 字段类型
     * @param name 字段名
     * @param comment 注释
     * @return 字段定义代码
     */
    public static String defineField(String type, String name, String comment) {
        StringBuilder sb = new StringBuilder();
        if (comment != null && !comment.isEmpty()) {
            sb.append("    /**\n");
            sb.append("     * ").append(comment).append("\n");
            sb.append("     */\n");
        }
        sb.append("    private ").append(type).append(" ").append(name).append(";\n");
        return sb.toString();
    }

    /**
     * 生成 Getter 方法
     *
     * @param type 返回类型
     * @param fieldName 字段名
     * @param capitalName 首字母大写的字段名
     * @return Getter 方法代码
     */
    public static String getter(String type, String fieldName, String capitalName) {
        return "    public " + type + " get" + capitalName + "() {\n" +
               "        return " + fieldName + ";\n" +
               "    }\n";
    }

    /**
     * 生成 Setter 方法
     *
     * @param type 参数类型
     * @param fieldName 字段名
     * @param capitalName 首字母大写的字段名
     * @return Setter 方法代码
     */
    public static String setter(String type, String fieldName, String capitalName) {
        return "    public void set" + capitalName + "(" + type + " " + fieldName + ") {\n" +
               "        this." + fieldName + " = " + fieldName + ";\n" +
               "    }\n";
    }

    /**
     * 生成 Getter/Setter 方法对
     *
     * @param type 类型
     * @param fieldName 字段名
     * @param capitalName 首字母大写的字段名
     * @param comment 注释
     * @return Getter/Setter 方法代码
     */
    public static String getterAndSetter(String type, String fieldName, 
                                         String capitalName, String comment) {
        StringBuilder sb = new StringBuilder();
        if (comment != null && !comment.isEmpty()) {
            sb.append("    /**\n");
            sb.append("     * ").append(comment).append("\n");
            sb.append("     * @return ").append(fieldName).append("\n");
            sb.append("     */\n");
        }
        sb.append(getter(type, fieldName, capitalName));
        sb.append("\n");
        sb.append(setter(type, fieldName, capitalName));
        return sb.toString();
    }

    /**
     * 生成 toString 方法
     *
     * @param className 类名
     * @param fieldNames 字段名列表
     * @return toString 方法代码
     */
    public static String toStringMethod(String className, List<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("    @Override\n");
        sb.append("    public String toString() {\n");
        sb.append("        return \"").append(className).append("{\" +\n");

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            sb.append("               \"").append(fieldName).append("=\" + ")
              .append(fieldName);
            if (i < fieldNames.size() - 1) {
                sb.append(" + \", \" +\n");
            } else {
                sb.append(" +\n");
            }
        }

        sb.append("               \"}\";\n");
        sb.append("    }\n");
        return sb.toString();
    }

    /**
     * 判断是否为最后一个元素
     *
     * @param index 当前索引
     * @param size 总大小
     * @return 是否为最后一个
     */
    public static boolean isLast(int index, int size) {
        return index == size - 1;
    }

    /**
     * 生成逗号分隔的字段列表
     *
     * @param fieldNames 字段名列表
     * @param prefix 前缀（可选）
     * @return 逗号分隔的字符串
     */
    public static String joinFields(List<String> fieldNames, String prefix) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return "";
        }
        return fieldNames.stream()
            .map(f -> prefix != null ? prefix + f : f)
            .collect(Collectors.joining(", "));
    }

    /**
     * 驼峰转下划线
     *
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名
     */
    public static String camelToUnderline(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    /**
     * 下划线转驼峰
     *
     * @param underline 下划线命名字符串
     * @return 驼峰命名
     */
    public static String underlineToCamel(String underline) {
        if (underline == null || underline.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        for (int i = 0; i < underline.length(); i++) {
            char ch = underline.charAt(i);
            if (ch == '_') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(ch));
                    nextUpperCase = false;
                } else {
                    result.append(ch);
                }
            }
        }
        return result.toString();
    }

    /**
     * 首字母大写
     *
     * @param str 字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 首字母小写
     *
     * @param str 字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 生成 MyBatis 映射字段
     *
     * @param fieldNames 字段名列表
     * @param tableAlias 表别名
     * @return 映射字段字符串
     */
    public static String mybatisSelectColumns(List<String> fieldNames, String tableAlias) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return "*";
        }
        String prefix = tableAlias != null ? tableAlias + "." : "";
        return fieldNames.stream()
            .map(f -> prefix + f)
            .collect(Collectors.joining(",\n        "));
    }

    /**
     * 生成 MyBatis 更新字段（排除主键）
     *
     * @param fieldNames 字段名列表
     * @param primaryKey 主键字段名
     * @return 更新字段字符串
     */
    public static String mybatisUpdateSet(List<String> fieldNames, String primaryKey) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return "";
        }
        return fieldNames.stream()
            .filter(f -> !f.equals(primaryKey))
            .map(f -> f + " = #{" + f + "}")
            .collect(Collectors.joining(",\n        "));
    }

    /**
     * 生成 MyBatis 插入字段
     *
     * @param fieldNames 字段名列表
     * @return 插入字段字符串
     */
    public static String mybatisInsertColumns(List<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return "";
        }
        return String.join(", ", fieldNames);
    }

    /**
     * 生成 MyBatis 插入值
     *
     * @param fieldNames 字段名列表
     * @return 插入值字符串
     */
    public static String mybatisInsertValues(List<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return "";
        }
        return fieldNames.stream()
            .map(f -> "#{" + f + "}")
            .collect(Collectors.joining(", "));
    }

    /**
     * 判断字符串是否为空
     *
     * @param str 字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str 字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 安全获取字符串（避免 NPE）
     *
     * @param str 字符串
     * @param defaultValue 默认值
     * @return 字符串或默认值
     */
    public static String safeGet(String str, String defaultValue) {
        return str != null ? str : defaultValue;
    }
}

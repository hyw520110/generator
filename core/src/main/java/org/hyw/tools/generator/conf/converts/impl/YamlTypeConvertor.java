package org.hyw.tools.generator.conf.converts.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyw.tools.generator.conf.KeyPair;
import org.hyw.tools.generator.conf.converts.TypeConvertor;
import org.hyw.tools.generator.constants.Consts;
import org.hyw.tools.generator.enums.FieldType;
import org.hyw.tools.generator.enums.db.DBType;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于 YAML 配置的类型转换器
 * 优先级：数据库特定配置 > 公共配置 > FieldType 默认兜底
 *
 * @author heyiwu
 */
@Slf4j
public class YamlTypeConvertor implements TypeConvertor {

    private final Map<String, FieldType> typeMap = new HashMap<>();
    private final DBType dbType;

    public YamlTypeConvertor(DBType dbType) {
        this.dbType = dbType;
        loadMappings();
    }

    private void loadMappings() {
        // 加载公共映射
        loadFromYaml(Consts.CONF_DIR_PATH + "common-types" + Consts.EXT_YML);
        
        // 加载数据库特定映射 
        String dbName = dbType.getName().toLowerCase();
        loadFromYaml(Consts.CONF_DIR_PATH + dbName + Consts.EXT_YML);
    }

    @SuppressWarnings("unchecked")
    private void loadFromYaml(String path) {
        try (InputStream is = YamlTypeConvertor.class.getResourceAsStream(path)) {
            if (is == null) return;
            Map<String, Object> yamlData = new Yaml().load(is);
            if (yamlData == null) return;

            Object mappings = yamlData.get("mappings");
            if (mappings instanceof Map) {
                Map<String, String> map = (Map<String, String>) mappings;
                map.forEach((dbTypeStr, javaTypeStr) -> {
                    FieldType fieldType = parseFieldType(javaTypeStr);
                    if (fieldType != null) {
                        typeMap.put(dbTypeStr.toLowerCase(), fieldType);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("无法从 {} 加载类型映射: {}", path, e.getMessage());
        }
    }

    private FieldType parseFieldType(String javaTypeStr) {
        if (StringUtils.isBlank(javaTypeStr)) return null;
        // 处理全类名或简写，提取最后的类名
        String simpleName = javaTypeStr.contains(".") ? StringUtils.substringAfterLast(javaTypeStr, ".") : javaTypeStr;
        String searchName = simpleName.toUpperCase().replace("[]", "_ARRAY");
        
        try {
            // 1. 尝试直接匹配枚举名
            return FieldType.valueOf(searchName);
        } catch (IllegalArgumentException e) {
            // 2. 模糊匹配别名
            if ("BIGDECIMAL".equals(searchName)) return FieldType.BIG_DECIMAL;
            if ("TIMESTAMP".equals(searchName)) return FieldType.TIMESTAMP;
            if ("DATETIME".equals(searchName)) return FieldType.TIMESTAMP; // MySQL datetime 映射到 Timestamp
            
            log.debug("未找到 FieldType 枚举对应类型: {}, 尝试使用默认兜底", javaTypeStr);
            return null;
        }
    }

    @Override
    public KeyPair<String, FieldType> convert(String columnType) {
        if (StringUtils.isBlank(columnType)) {
            return new KeyPair<>("UNKNOWN", FieldType.STRING);
        }
        // 1. 剥离长度信息，如 varchar(255) -> varchar
        String baseType = StringUtils.substringBefore(columnType, "(").toLowerCase().trim();
        // 2. 剥离 unsigned 关键字，如 int unsigned -> int
        baseType = baseType.replace("unsigned", "").trim();
        
        FieldType fieldType = typeMap.get(baseType);

        if (fieldType == null) {
            log.debug("未定义的列类型映射: {}, 原始类型: {}, 使用 STRING 兜底", baseType, columnType);
            fieldType = FieldType.STRING;
        }

        // 返回 KeyPair (JDBC Type 暂时沿用剥离后的基础类型名)
        return new KeyPair<>(baseType.toUpperCase(), fieldType);
    }
}

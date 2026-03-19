package org.hyw.tools.generator.conf.db;

import java.util.List;
import java.util.stream.Collectors;
import org.hyw.tools.generator.utils.StringUtils;

/**
 * 复合主键信息封装
 */
public class PrimaryKeyInfo {
    private final boolean composite;
    private final List<TabField> fields;
    private final String keyClassName;
    private final String keyClassPackage;
    
    public PrimaryKeyInfo(List<TabField> fields, String entityPackage) {
        this.fields = fields;
        this.composite = fields.size() > 1;
        
        if (composite && !fields.isEmpty()) {
            String tableName = fields.get(0).getName();
            String entityName = StringUtils.removePrefixAndCamel(tableName, new String[]{"t_", "tb_"}, new char[]{'_'});
            this.keyClassName = entityName + "Key";
            this.keyClassPackage = entityPackage + ".key";
        } else {
            this.keyClassName = null;
            this.keyClassPackage = null;
        }
    }

    public boolean isComposite() { return composite; }
    public List<TabField> getFields() { return fields; }
    public String getKeyClassName() { return keyClassName; }
    public String getKeyClassPackage() { return keyClassPackage; }
    
    public int size() { return fields.size(); }
    public TabField getFirstField() { return fields.isEmpty() ? null : fields.get(0); }
    
    public String getKeyType() {
        if (composite) return keyClassName;
        return getFirstField() != null ? getFirstField().getPropertyType() : "Object";
    }
    
    public String getFieldNames() {
        return fields.stream().map(TabField::getName).collect(Collectors.joining(", "));
    }
    
    public String getPropertyNames() {
        return fields.stream().map(TabField::getPropertyName).collect(Collectors.joining(", "));
    }
    
    public String getParameterList() {
        return fields.stream().map(f -> f.getPropertyType() + " " + f.getPropertyName()).collect(Collectors.joining(", "));
    }
    
    public String getValueList() {
        return fields.stream().map(TabField::getPropertyName).collect(Collectors.joining(", "));
    }
}

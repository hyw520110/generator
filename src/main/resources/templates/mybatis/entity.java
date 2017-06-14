package ${package.Entity};

#foreach($pkg in ${table.importPackages})
import ${pkg};
#end

/**
 * ${table.name}：$!{table.comment}
 * @author ${author}
 * @since ${date}
 */
#if(${superEntityClass})
public class ${table.entityName} extends ${superEntityClass} {
#else
public class ${table.entityName} implements Serializable {
#end

    private static final long serialVersionUID = 1L;

#foreach($field in ${table.fields})
#if(${field.keyFlag})
#set($keyPropertyName=${field.propertyName})
#end
#if("$!field.comment" != "")
    /**
     * ${field.comment}
     */
#end
#if(${field.keyFlag})
    private ${field.propertyType} ${field.propertyName};
#end

#foreach($field in ${table.fields})
#if(${field.propertyType.equals("Boolean")})
#set($getprefix="is")
#else
#set($getprefix="get")
#end
#end
    public ${field.propertyType} ${getprefix}${field.capitalName}() {
        return ${field.propertyName};
    }

#if(${entityBuilderModel})
    public ${entity} set${field.capitalName}(${field.propertyType} ${field.propertyName}) {
#else
    public void set${field.capitalName}(${field.propertyType} ${field.propertyName}) {
#end
        this.${field.propertyName} = ${field.propertyName};
#if(${entityBuilderModel})
        return this;
#end
    }
#end

#if(${entityColumnConstant})
#foreach($field in ${table.fields})
    public static final String ${field.name.toUpperCase()} = "${field.name}";

#end
#end
#if(${activeRecord})
    protected Serializable getPK() {
#if(${keyPropertyName})
        return this.${keyPropertyName};
#else
        return this.id;
#end
    }
#end
}
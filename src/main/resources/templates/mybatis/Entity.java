package ${rootPackage}.${projectName}.${packageEntity};

#foreach($pkg in ${table.importPackages})
import ${pkg};
#end

/**
 * ${table.name}：$!{table.comment}
 * @author ${author}
 * @since ${date}
 */
public class ${EntityName} #if(${superEntityClass}) extends ${superEntityClass} #else implements Serializable #end{

    private static final long serialVersionUID = 1L;

#foreach($field in ${table.fields})
#if("$!field.comment" != "")
    /**
    * ${field.comment}
    */
#end
    private ${field.fieldType.type} ${field.propertyName};
#end

#foreach($field in ${table.fields})
    #if(${field.propertyType.equals("Boolean")})
    #set($getprefix="is")
    #else
    #set($getprefix="get")
    #end

    public ${field.fieldType.type} ${getprefix}${field.capitalName}() {
        return ${field.propertyName};
    }

    public void set${field.capitalName}(${field.fieldType.type} ${field.propertyName}) {
        this.${field.propertyName} = ${field.propertyName};
    }
#end
}
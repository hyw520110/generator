package ${entityPackage};

#foreach($pkg in ${table.importPackages})
import ${pkg};
#end
import java.io.Serializable;    
import ${persistencePackage}.Entity;
import ${persistencePackage}.GeneratedValue;
import ${persistencePackage}.GenerationType;
import ${persistencePackage}.Id;
import ${validationPackage}.constraints.NotBlank;
import ${validationPackage}.constraints.NotNull;

#parse('/templates/comments/comment.vm')
@Entity
public class ${className} implements Serializable{

    private static final long serialVersionUID = 1L;

#foreach($field in ${table.fields})
#if("$!field.comment" != "")
    /**
    * ${field.comment}
    */
#end
#if(${field.isPrimarykey()})
	@Id @GeneratedValue(strategy=GenerationType.AUTO)
#end
#if(!$field.isNullAble())#if($field.propertyType == "String")    @NotBlank#else    @NotNull#end
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

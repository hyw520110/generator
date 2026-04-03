package ${dtoPackage};

import java.io.Serializable;
#foreach($pkg in $table.importPackages)
#if($pkg && !$pkg.contains("jakarta.validation") && !$pkg.contains("org.apache.commons"))
import ${pkg};
#end
#end

import io.swagger.v3.oas.annotations.media.Schema;

#parse('comments/comment.vm')
#if table.comment?has_content>
@Schema(name = "${dtoName}", description = "${table.comment!}")
#end
public class ${dtoName} implements Serializable {

    private static final long serialVersionUID = 1L;

#foreach($field in $table.fields)

#if($field.comment)

    /**
    * ${field.comment}
    */

#end

	@Schema(name = "${field.propertyName}", description = #if($field.comment)"${field.comment}"#else"${field.name}"#end)
    private ${field.fieldType.type} ${field.propertyName};
    
#end

#foreach($field in $table.fields)
    #if($field.propertyType == "Boolean")
    #set($getprefix = "is")
    #else
    #set($getprefix = "get")
    #end

    public ${field.fieldType.type} ${getprefix}${field.capitalName}() {
        return ${field.propertyName};
    }

    public void set${field.capitalName}(${field.fieldType.type} ${field.propertyName}) {
        this.${field.propertyName} = ${field.propertyName};
    }
#end

	public String toString() {
	    return "${dtoName}{" +
		#foreach($field in $table.fields)
		"#if($foreach.index > 0), #end${field.propertyName}="+${field.propertyName}+
		#end "}";
	}
}

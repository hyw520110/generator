package ${entityPackage};


#foreach($pkg in ${table.importPackages})
import ${pkg};
#end
#if(${superEntityClass})
import #if($StringUtils.indexOf("$superEntityClass",'.')==-1)${entityPackage}.#end$superEntityClass;
#else
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import com.mysql.jdbc.StringUtils;    
#end
#if("plus"=="$mapperType")
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
#end
import javax.validation.constraints.NotNull;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

#parse('/templates/commons/comment.vm')
#if(${table.comment})
@ApiModel(value="${table.name}", description="${table.comment}")
#end
#if("plus"=="$mapperType")
@TableName("${table.name}")
#end
public class ${className} #if(${superEntityClass}) extends ${StringUtils.getClassName("$superEntityClass")}#if("plus"=="$mapperType")<${className}>#end #else implements Serializable #end{

    private static final long serialVersionUID = 1L;

#foreach($field in ${table.fields})
#if("$!field.comment" != "")
    /**
    * ${field.comment}
    */
#end
#if("plus"=="$mapperType"&&$field.isPrimarykey)
	@TableId(value = "${field.name}", type = IdType.AUTO)
#end
	@ApiModelProperty(value = #if("$!{field.comment}"!="")"$!{field.comment}"#else"${field.name}"#end,required = #if(${field.isNullAble()})false #else true #end)
#if(!${field.isCommonField} || ${StringUtils.indexOf("$superEntityClass", '.')}!=-1)
#if(!$field.isNullAble())    @NotNull
#end 
    private ${field.fieldType.type} ${field.propertyName};
    
#end    
#end
## TODO  外键关联配置 引用对象
## 如 一对一,当前为order订单类，一个订单对应一个人，则此处应引入person
## 如多对一,当前为person类，一个人对应多个订单，则此处引入List<Order> orders

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
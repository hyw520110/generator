package ${entityPackage};

#foreach($pkg in ${table.importPackages})
import ${pkg};
#end
#if(${superEntityClass})
import #if($StringUtils.indexOf("$superEntityClass",'.')==-1)${entityPackage}.#end$superEntityClass;
#else
import java.io.Serializable;    
#end


#parse('/templates/commons/comment.vm')
public class ${className} #if(${superEntityClass}) extends ${StringUtils.getClassName($superEntityClass)} #else implements Serializable #end{

    private static final long serialVersionUID = 1L;

#foreach($field in ${table.fields})
#if("$!field.comment" != "")
    /**
    * ${field.comment}
    */
#end
    private ${field.fieldType.type} ${field.propertyName};
## TODO  外键关联配置 引用对象
## 如 一对一,当前为order订单类，一个订单对应一个人，则此处应引入person
## 如多对一,当前为person类，一个人对应多个订单，则此处引入List<Order> orders
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
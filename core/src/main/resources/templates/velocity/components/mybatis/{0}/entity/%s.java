package ${entityPackage};


#foreach($pkg in ${table.importPackages})
import ${pkg};
#end
#if(${superEntityClass})
import #if($StringUtils.indexOf("$superEntityClass",'.')==-1)${entityPackage}.#end$superEntityClass;
#else
import java.io.Serializable;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
#end
#if("plus"=="$mapperType")
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
#end
import jakarta.validation.constraints.NotNull;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

#parse('/templates/comments/comment.vm')
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
#if("plus"=="$mapperType")
#if($field.isPrimarykey)
	@TableId(value = "${field.name}", type = IdType.AUTO)
#else
	@TableField(value = "${field.name}")
#end
#end
	@ApiModelProperty(value = #if("$!{field.comment}"!="")"$!{field.comment}"#else"${field.name}"#end,required = #if(${field.isNullAble()})false #else true #end)
#if(!${field.isCommonField} || ${StringUtils.indexOf("$superEntityClass", '.')}!=-1)
#if(!$field.isNullAble())    @NotNull
#end 
    private ${field.fieldType.type} ${field.propertyName};
    
#end    
#end
## TODO  外键关联配置 引用对象
##
## 使用说明：
## 在此处添加外键关联的对象属性，用于关联查询和级联操作
##
## 一对一关系示例：
## 当前为 Order 订单类，一个订单对应一个 Person，则添加：
##     private Person person;
##
## 多对一关系示例：
## 当前为 OrderItem 订单明细类，多个明细对应一个 Order，则添加：
##     private Order order;
##
## 一对多关系示例：
## 当前为 Person 类，一个人对应多个 Order，则添加：
##     private List<Order> orders;
##     // 注意：一对多通常使用 @TableField(exist = false) 标注，避免映射到数据库
##     // 查询时需要使用额外的 SQL 或 MyBatis 的 collection 映射
##
## 多对多关系示例：
## 当前为 Student 学生类，学生与 Course 课程多对多，则添加：
##     private List<Course> courses;
##     // 注意：多对多关系需要中间表，通常使用 @TableField(exist = false)
##     // 需要在 Mapper 中定义额外的查询方法或使用 @Many 注解
##
## 注意事项：
## 1. 添加关联对象属性时，应使用 @TableField(exist = false) 标注非数据库字段
## 2. 对于 MyBatis-Plus，可以使用 @TableField(select = false) 避免自动查询
## 3. 复杂的关联查询建议在 Mapper XML 中定义，避免 N+1 查询问题
## 4. 级联操作（级联删除、级联更新）需要在 Service 层处理，不建议在实体类中直接处理

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
#if("plus"=="$mapperType")
#if($table.hasPrimarykeys())
	@Override
	protected $table.primaryKeyField.propertyType pkVal() {
	    return this.$table.primaryKeyField.propertyName;
	}
#end
	public String toString() {
	    return "${className}{" +
		#foreach($field in ${table.fields})
		"#if($foreach.count>1), #end${field.propertyName}="+${field.propertyName}+
		#end "}";
	}
#end
}
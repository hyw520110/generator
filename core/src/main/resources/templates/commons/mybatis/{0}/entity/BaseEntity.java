#if(${StringUtils.indexOf("$superEntityClass", '.')}==-1)
package ${entityPackage};

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
#if("plus"=="$mapperType")
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.extension.activerecord.Model;
#end

import java.util.Date;  

#set($comment="公共实体类/公有属性")
#parse('/templates/comments/comment.vm')
public abstract class BaseEntity#if("plus"=="$mapperType")<T extends BaseEntity> extends Model<T>#else<T>#end implements Serializable  {

    private static final long serialVersionUID = 1L;
	 
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
#if("plus"=="$mapperType")
	@Override
	protected Serializable pkVal() {
		return super.pkVal();
	}
#end    	
}
#end
<#if superEntityClass?? && !superEntityClass?contains('.')>
package ${entityPackage!};

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
<#if "plus"=="mapperType">
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.extension.activerecord.Model;
</#if>

import java.util.Date;

<#assign comment ="公共实体类/公有属性">
<#include 'comments/comment.ftl'>
<#if "plus"=="mapperType">
public abstract class BaseEntity<T extends BaseEntity<T>> extends Model<T> implements Serializable  {
<#else>
public abstract class BaseEntity<T> implements Serializable  {
</#if>
    private static final long serialVersionUID = 1L;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
<#if "plus"=="mapperType">
	@Override
	protected Serializable pkVal() {
		return super.pkVal();
	}
</#if>
}
</#if>
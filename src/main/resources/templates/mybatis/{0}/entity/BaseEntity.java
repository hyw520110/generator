#if(${StringUtils.indexOf("$superEntityClass", '.')}==-1)
package ${entityPackage};

import java.io.Serializable;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import java.util.Date;  


/**
 * @author ${author}
 * @since ${date}
 * @copyright: ${copyright}
 */
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;
	//创建时间 
	private Date createTime;
	//最后修改时间 
	private Date lastModifyTime; 	

	public Date getCreateTime() {  
	    return createTime;  
	}  
	
	public void setCreateTime(Date createTime) {  
	    this.createTime = createTime;  
	}  
	
	public Date getLastModifyTime() {  
	    return lastModifyTime;  
	}  
	
	public void setLastModifyTime(Date lastModifyTime) {  
	    this.lastModifyTime = lastModifyTime;  
	}  
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
}
#end
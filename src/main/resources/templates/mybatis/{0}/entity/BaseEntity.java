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
	 
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
}
#end
package ${entityPackage};

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * @author ${author}
 * @since ${date}
 * @copyright: ${copyright}
 */
public class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;
#if(${StringUtils.indexOf("$superEntityClass", '.')}==-1)
#foreach($field in ${table.fields})	
	private ${field.fieldType.type} ${field.propertyName};
#end
#end    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
}

<#if sqlType?? && sqlType == "plus">
package ${plusPackage!};

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.slf4j.MDC;
import java.util.Date;

@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {
	private static final Logger logger = LoggerFactory.getLogger(MybatisPlusMetaObjectHandler.class);
    @Override
    public void insertFill(MetaObject metaObject) {
    	logger.debug("metaObject:{}",metaObject);
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);
        
        String userId = MDC.get("userId");
        if (userId != null && !userId.isEmpty()) {
            this.setFieldValByName("createBy", userId, metaObject);
            this.setFieldValByName("updateBy", userId, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
    	logger.debug("metaObject:{}",metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);
        
        String userId = MDC.get("userId");
        if (userId != null && !userId.isEmpty()) {
            this.setFieldValByName("updateBy", userId, metaObject);
        }
    }
}
</#if>
#if("plus"=="$mapperType")
package ${plusPackage};

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

//@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {
	private static final Logger logger = LoggerFactory.getLogger(MybatisPlusConfiguration.class);
	//TODO 通用
    @Override
    public void insertFill(MetaObject metaObject) {
    	logger.debug("metaObject:{}",metaObject);
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);

    }

    @Override
    public void updateFill(MetaObject metaObject) {
    	logger.debug("metaObject:{}",metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);

    }
}
#end
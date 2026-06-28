package ${packagePath};

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 自定义数据权限处理器
 */
@Component
public class CustomDataPermissionHandler implements DataPermissionHandler {

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        // TODO: 解析 mappedStatementId 拿到具体的方法和注解，判断是否需要拼装数据权限 SQL
        // 返回追加的条件 SQL 片段，这里做个示范
        return new StringValue(" 1=1 /* data permission */ ");
    }
}

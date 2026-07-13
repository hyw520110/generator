package ${packagePath};

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.stereotype.Component;
import ${global.rootPackage}.${global.projectName}.${moduleName}.annotation.DataPermission;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 自定义数据权限处理器
 */
@Component
public class CustomDataPermissionHandler implements DataPermissionHandler {

    private final Map<String, DataPermission> permissionCache = new ConcurrentHashMap<>();

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        DataPermission annotation = permissionCache.computeIfAbsent(mappedStatementId, id -> {
            try {
                int lastDotIndex = id.lastIndexOf(".");
                String className = id.substring(0, lastDotIndex);
                String methodName = id.substring(lastDotIndex + 1);
                Class<?> clazz = Class.forName(className);
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(methodName) && m.isAnnotationPresent(DataPermission.class)) {
                        return m.getAnnotation(DataPermission.class);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
            // Use an anonymous inner class instance to represent 'null' since ConcurrentHashMap doesn't allow null values
            return new DataPermission() {
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() { return DataPermission.class; }
                @Override
                public String deptAlias() { return "NONE"; }
                @Override
                public String userAlias() { return "NONE"; }
            };
        });

        if ("NONE".equals(annotation.deptAlias())) {
            return where; // 无需过滤
        }

        // 示范：拼接对应表的 alias 进行数据权限控制
        String alias = annotation.deptAlias();
        String sql = alias + ".dept_id IN (SELECT dept_id FROM sys_user_dept WHERE user_id = 1)"; // 示例SQL
        
        return new StringValue(sql);
    }
}

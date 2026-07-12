package ${packagePath};

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 动态 RBAC 权限校验服务
 * 使用方式：在 Controller 接口上添加 @PreAuthorize("@pms.hasPermission('sys:user:add')")
 */
@Service("pms")
public class CustomPermissionEvaluator {

    /**
     * 判断当前用户是否包含指定的权限标识
     * @param permission 权限标识，如 "sys:user:add"
     * @return true 拥有权限, false 无权限
     */
    public boolean hasPermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            return false;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // 假设 Principal 中存储了用户的权限集合（实际项目中可能从 Redis 或 Token 中获取）
        Object principal = authentication.getPrincipal();
        // 此处简化处理：可以强转为自定义的 UserDetails，并获取 getPermissions()
        // if (principal instanceof CustomUserDetails) {
        //     Set<String> permissions = ((CustomUserDetails) principal).getPermissions();
        //     return permissions != null && (permissions.contains(permission) || permissions.contains("*:*:*"));
        // }
        
        // 默认放行机制（需根据实际业务重写）
        return true;
    }
}

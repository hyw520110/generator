package ${packagePath};

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 多端认证授权端点
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired(required = false)
    private AuthenticationManager authenticationManager;

    /**
     * 账号密码登录
     */
    @PostMapping("/login")
    public Object login(@RequestBody LoginRequest request) {
        if (authenticationManager == null) {
            return "AuthenticationManager 未配置";
        }
        
        // 1. 创建认证 Token
        UsernamePasswordAuthenticationToken token = 
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
            
        // 2. 执行认证
        Authentication authentication = authenticationManager.authenticate(token);
        
        // 3. 生成 JWT (此处简化演示，实际应集成 JWTUtil)
        // String jwt = JWTUtil.createJWT(authentication.getName(), 3600000L);
        String jwt = "mock-jwt-token-for-" + authentication.getName();
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", jwt);
        result.put("username", authentication.getName());
        return result;
    }

    /**
     * 短信验证码登录 (预留接口)
     */
    @PostMapping("/sms-login")
    public Object smsLogin(@RequestBody LoginRequest request) {
        // 自定义 SmsAuthenticationToken 并使用 authenticationManager 认证
        return "短信登录成功";
    }
}

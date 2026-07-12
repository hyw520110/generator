package ${packagePath};

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 自定义用户信息加载逻辑
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 模拟从数据库加载用户
        if (!"admin".equals(username) && !"user".equals(username)) {
            throw new UsernameNotFoundException("用户不存在");
        }

        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // 实际项目中密码应当从数据库查询出来（已经被 BCrypt 等加密）
        String encodedPassword = encoder.encode("123456");

        return User.builder()
                .username(username)
                .password(encodedPassword)
                .authorities("admin".equals(username) ? "ROLE_ADMIN" : "ROLE_USER")
                .build();
    }
}

package ${packagePath};

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class OAuth2ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> {
                <#if platformId?? && platformId?contains("boot3")>
                authorize.requestMatchers("/public/**").permitAll()
                    .anyRequest().authenticated();
                <#else>
                authorize.antMatchers("/public/**").permitAll()
                    .anyRequest().authenticated();
                </#if>
            })
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
            }));
        return http.build();
    }
}

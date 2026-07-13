package ${packagePath};

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

<#if springBootMajor?? && springBootMajor != "2">
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
<#else>
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
</#if>

@Configuration
@EnableWebSecurity
<#if springBootMajor?? && springBootMajor != "2">
@EnableMethodSecurity
<#else>
@EnableGlobalMethodSecurity(prePostEnabled = true)
</#if>
public class SecurityConfig {

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                <#if springBootMajor?? && springBootMajor != "2">
                auth.requestMatchers("/api/auth/**", "/auth/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().permitAll();
                <#else>
                auth.antMatchers("/api/auth/**").permitAll()
                    .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().permitAll();
                </#if>
            });
        return http.build();
    }
}

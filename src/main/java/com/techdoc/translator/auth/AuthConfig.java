package com.techdoc.translator.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로그인 검사 설정. app.auth.enabled=true일 때만 /api/** 에 토큰 검사를 건다.
 * 꺼져 있으면(기본값) 지금처럼 임시 회원(dev@local)으로 동작한다.
 */
@Configuration
public class AuthConfig implements WebMvcConfigurer {

    private final boolean enabled;
    private final JwtService jwtService;

    public AuthConfig(@Value("${app.auth.enabled:false}") boolean enabled, JwtService jwtService) {
        this.enabled = enabled;
        this.jwtService = jwtService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (enabled) {
            registry.addInterceptor(new AuthInterceptor(jwtService)).addPathPatterns("/api/**");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

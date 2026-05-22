package com.campushub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器（P3 §9.1：BCrypt cost=10）。
 *
 * 只暴露 PasswordEncoder bean，**不引入 spring-boot-starter-security**
 * 避免 CSRF / formLogin / basicAuth 等默认配置干扰当前纯 JWT 鉴权模型
 * （决策详见 SecurityConfig 注释）。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}

package com.campushub.config;

import org.springframework.context.annotation.Configuration;

/**
 * 安全配置占位。
 *
 * 当前阶段（W10 ~ W14）的鉴权与白名单完全由 com.campushub.common.interceptor.JwtAuthInterceptor 实现：
 *   - 白名单：campushub.security.whitelist（CSV，逗号分隔的 Ant 模式）
 *   - 鉴权：Bearer Token → JwtUtil.parse → 写入 CurrentUserHolder
 *
 * 暂不引入 spring-boot-starter-security 的理由：
 *   1. Spring Security 默认会启用 CSRF / formLogin / basicAuth，会和我们的纯 JWT 模型冲突
 *   2. 需要写 SecurityFilterChain 把所有路径 permitAll 后再交给拦截器，等于绕了一圈
 *   3. P0 阶段功能优先级更高；如未来要走 OAuth2 ResourceServer，再加该 starter 并把鉴权迁移过来
 *
 * 若启用 Spring Security，请：
 *   1. pom 加 spring-boot-starter-security
 *   2. 在本类暴露 SecurityFilterChain Bean：禁用 csrf / 设 SessionCreationPolicy.STATELESS /
 *      permitAll 所有路径（路径鉴权依然交给拦截器，避免双重权限语义）
 */
@Configuration
public class SecurityConfig {
}

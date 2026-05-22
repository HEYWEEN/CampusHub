package com.campushub.config;

import com.campushub.common.interceptor.JwtAuthInterceptor;
import com.campushub.common.interceptor.TraceIdInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 Web 配置：
 *   - 注册 TraceId / JwtAuth 拦截器
 *   - 配置 CORS（前端 dev 默认 5173 端口）
 *   - 启用 JwtProperties 绑定
 *
 * 拦截器顺序很重要：TraceId 必须先于 JwtAuth —— 鉴权失败抛 BizException 时
 * GlobalExceptionHandler 拼装的 ApiResponse 才能带上 traceId。
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final TraceIdInterceptor traceIdInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    public WebMvcConfig(TraceIdInterceptor traceIdInterceptor, JwtAuthInterceptor jwtAuthInterceptor) {
        this.traceIdInterceptor = traceIdInterceptor;
        this.jwtAuthInterceptor = jwtAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .order(1);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

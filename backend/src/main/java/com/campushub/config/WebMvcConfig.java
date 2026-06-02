package com.campushub.config;

import com.campushub.common.interceptor.AdminAuthInterceptor;
import com.campushub.common.interceptor.JwtAuthInterceptor;
import com.campushub.common.interceptor.TraceIdInterceptor;
import com.campushub.common.storage.ImageStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final ImageStorage imageStorage;

    public WebMvcConfig(TraceIdInterceptor traceIdInterceptor,
                        JwtAuthInterceptor jwtAuthInterceptor,
                        AdminAuthInterceptor adminAuthInterceptor,
                        ImageStorage imageStorage) {
        this.traceIdInterceptor = traceIdInterceptor;
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.imageStorage = imageStorage;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor)
                .addPathPatterns("/**")
                .order(0);

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        // 管理端：先过 jwt(order 1) 拿到 userId，再校验 ADMIN
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(2);
    }

    /**
     * 静态资源映射：把 /uploads/** 映射到本地图床目录，让浏览器能 <img src="/uploads/xx.jpg">。
     * 路径不带 /api 前缀，所以不会被 JwtAuthInterceptor 拦截，无需登录即可访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + imageStorage.getBaseDir().toString() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
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

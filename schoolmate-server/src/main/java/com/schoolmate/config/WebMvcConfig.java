package com.schoolmate.config;

import com.schoolmate.interceptor.JwtInterceptor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：CORS 跨域、静态资源映射、JWT 拦截器注册。
 *
 * @author Albot
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** 前端开发服务器地址 */
    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:8081"
    };

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Value("${schoolmate.upload.base-dir}")
    private String uploadBaseDir;

    @Value("${schoolmate.upload.access-prefix}")
    private String uploadAccessPrefix;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(ALLOWED_ORIGINS)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }

    /** 上传目录映射为静态资源，供前端直接访问图片 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadBaseDir.startsWith("file:") ? uploadBaseDir : "file:" + uploadBaseDir + "/";
        registry.addResourceHandler(uploadAccessPrefix).addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                // 认证相关
                "/auth/**",
                // 接口文档
                "/doc.html",
                "/webjars/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/favicon.ico",
                // 静态资源与错误转发
                "/upload/**",
                "/error"
            );
    }
}

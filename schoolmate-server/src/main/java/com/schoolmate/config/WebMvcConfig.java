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

    /**
     * CORS 允许的来源。
     *
     * <p>本项目的部署形态是「浏览器只访问一个端口（前端 5173），由 Vite 代理把 /api 转发给后端」，
     * 对浏览器而言前后端本就同源，正常情况根本不会触发 CORS 校验。但 Vite 代理设置了
     * {@code changeOrigin: true}，会把 Host 改写成 {@code localhost:8080}；而浏览器在
     * <b>非 GET 请求</b>上一定会带 Origin（例如 {@code http://xxx.natappfree.cc}），
     * 于是 Spring 将同源请求误判为跨域，白名单匹配失败 → 403 {@code Invalid CORS request}。
     * 表现为：所有 GET 页面/列表正常，但一点「登录」等写操作就报网络错误。
     *
     * <p>而内网穿透域名是动态的（natapp 免费隧道每次分配、cloudflared 每次重启都会变），
     * 无法预先枚举，因此这里放开来源。
     *
     * <p>安全性说明：鉴权走 Authorization 头里的 JWT，全项目不使用 Cookie / Session，
     * 且下面 {@code allowCredentials(false)}，浏览器不会携带任何凭据，
     * 第三方站点无法借用户身份调用接口。
     */
    private static final String[] ALLOWED_ORIGIN_PATTERNS = {"*"};

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Value("${schoolmate.upload.base-dir}")
    private String uploadBaseDir;

    @Value("${schoolmate.upload.access-prefix}")
    private String uploadAccessPrefix;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            // 全项目不使用 Cookie/Session，鉴权只靠 Authorization 头的 JWT，
            // 因此关闭凭据传输：既满足 allowCredentials 语义，也避免放开来源带来的跨站风险。
            .allowCredentials(false)
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
                // 认证相关：登录、注册、找回密码放行，/auth/me 需要鉴权
                "/auth/login",
                "/auth/register",
                "/auth/forgot-password",
                "/auth/reset-password",
                "/auth/mail-reset-enabled",
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
                "/error",
                // WebSocket 握手是一次 HTTP 请求，若走 JWT 拦截器必然失败
                // （浏览器原生 WebSocket API 无法携带 Authorization 头）。
                // 该路径改由 WsHandshakeInterceptor 在握手阶段用 ?token= 单独鉴权。
                "/ws/**"
            );
    }
}

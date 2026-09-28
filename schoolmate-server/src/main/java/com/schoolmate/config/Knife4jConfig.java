package com.schoolmate.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / springdoc 接口文档配置。
 *
 * <p>文档地址：http://localhost:8080/api/doc.html
 *
 * @author Albot
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI schoolmateOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("大学同学录 API 文档")
                .version("v1.0.0")
                .description("Spring Boot 3 三层架构 + RESTful API，Token 通过 Authorization 请求头传递"))
            // 声明全局鉴权参数，文档调试时可直接填入 Token
            .addSecurityItem(new SecurityRequirement().addList("Authorization"))
            .schemaRequirement("Authorization", new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER));
    }
}

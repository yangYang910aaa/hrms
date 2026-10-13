package com.yang.hr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j（OpenAPI3）接口文档配置。
 *
 * 启动后访问 {@code /doc.html}（不在 Sa-Token 的 /api/** 拦截范围，无需登录）：
 * - 全部接口按 Controller 自动分组，可直接在页面上"发送"调试；
 * - 调试时在页面参数里加请求头 satoken，值为登录接口返回的 tokenValue；
 * - OpenAPI JSON 在 /v3/api-docs，M7 交接口文档可直接导出。
 *
 * 约定提醒：HTTP 状态码恒 200，成败看响应体 {code,msg,data} 的 code。
 * ⚠️ 注释里别写星号加斜杠连写的路径样式（会提前闭合注释），一律用 {@code} 包起来。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI hrOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("人力资源管理系统 API")
                .version("1.0.0")
                .description("Spring Boot 3.2 + Sa-Token 鉴权。请求头 satoken 携带 token；HTTP 恒 200，成败看响应体 code。")
                .contact(new Contact().name("hr-backend")));
    }
}

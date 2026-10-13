package com.yang.hr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置。
 *
 * 用 CorsFilter（Servlet Filter）而不是 WebMvcConfigurer.addCorsMappings，
 * 原因是 Filter 在 DispatcherServlet 之前执行，浏览器发来的 OPTIONS 预检请求会被它直接应答并中断过滤器链，
 * **永远不会走到 Sa-Token 的登录拦截器**；否则预检拿不到 CORS 头、还被判 401，前端所有跨域请求都会失败。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 开发期放开；上线时把这里收紧成前端域名，例如 http://hr.example.com
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

package com.yang.hr.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置：/api/** 一律要求登录，白名单除外。
 *
 * 白名单里加了 /error —— 容器内部转发到错误页时若被拦下，会把真实错误码盖成 401，排查会很痛苦。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final String[] WHITE_LIST = {
            "/api/auth/login",
            "/api/ping",
            "/error"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(WHITE_LIST);
    }
}

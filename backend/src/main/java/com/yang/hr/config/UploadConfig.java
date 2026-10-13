package com.yang.hr.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.MultipartConfigElement;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传配置：文件大小上限 + 上传目录的静态资源映射。
 *
 * 两个刻意的选择：
 * 1. **大小上限写在 Java 里（`MultipartConfigElement`）而不是 application.yml** ——
 *    自定义该 Bean 会让 Spring Boot 的 `MultipartAutoConfiguration` 自动退让，效果等价，且不必动那个含凭据的配置文件。
 * 2. **资源位置用 `Path.toUri()` 生成**而不是拼 `"file:" + 路径` ——
 *    本项目路径含中文，手拼成 `file:D:/人力资源管理系统/uploads/` 是个非法 URI，中文可能被错误编码导致 404；
 *    `toUri()` 产出规范的 `file:///...` 百分号编码 URI，稳。
 *
 * ⚠️ `/uploads/**` 只被本类映射，**不在 Sa-Token 拦截范围**（拦截器只覆盖 `/api/**`），
 * 也就是说上传后的图片凭 URL 可直接访问、不需要登录。课程项目可接受；生产环境应改为鉴权或签名 URL。
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(UploadConfig.class);

    /** 对外访问前缀。FileStorageService 拼返回 URL 时引用的就是这个常量，两边必须一致 */
    public static final String URL_PREFIX = "/uploads";

    /**
     * 解析上传根目录 —— **静态映射与写盘必须共用这一个来源**。
     * 两边各算一遍的话，一旦不一致，症状就是"文件能上传、但访问不到"。
     */
    public static Path resolveRoot(String uploadDir) {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 上传根目录：默认项目下的 uploads/，可用 hr.upload.dir（或环境变量 HR_UPLOAD_DIR）覆盖 */
    @Value("${hr.upload.dir:./uploads}")
    private String uploadDir;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(5));
        factory.setMaxRequestSize(DataSize.ofMegabytes(10));
        return factory.createMultipartConfig();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(URL_PREFIX + "/**")
                .addResourceLocations(resolveRoot(uploadDir).toUri().toString());
    }

    @PostConstruct
    public void logResolvedDir() {
        log.info("上传目录（绝对路径）：{}", resolveRoot(uploadDir));
    }
}

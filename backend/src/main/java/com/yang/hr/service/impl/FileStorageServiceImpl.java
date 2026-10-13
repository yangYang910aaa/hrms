package com.yang.hr.service.impl;

import com.yang.hr.common.BizException;
import com.yang.hr.common.ResultCode;
import com.yang.hr.config.UploadConfig;
import com.yang.hr.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务实现：存盘 + 返回可访问 URL。
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    /** 图片扩展名白名单 —— 只放行扩展名，杜绝上传 .jsp/.exe/.sh 之类 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyyMM");

    private final Path root;

    public FileStorageServiceImpl(@Value("${hr.upload.dir:./uploads}") String uploadDir) {
        // 与 UploadConfig 的静态资源映射共用同一个解析方法，避免"能上传但访问不到"
        this.root = UploadConfig.resolveRoot(uploadDir);
    }

    @Override
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "上传文件为空");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "只支持图片格式：" + String.join(" / ", ALLOWED_EXTENSIONS));
        }

        // 文件名完全由服务端生成（UUID），不采信原始文件名 —— 天然规避路径穿越
        String monthDir = LocalDate.now().format(MONTH_DIR);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path dir = root.resolve(monthDir);
        Path target = dir.resolve(filename).normalize();

        // 兜一层：万一上面的拼装被改坏，也不允许写到 uploads 之外
        if (!target.startsWith(root)) {
            throw new BizException(ResultCode.PARAM_ERROR, "非法的文件路径");
        }

        try {
            Files.createDirectories(dir);
            file.transferTo(target);
        } catch (IOException | IllegalStateException e) {
            log.error("[文件保存失败] target={}", target, e);
            throw new BizException(ResultCode.ERROR, "文件保存失败，请重试");
        }
        log.info("上传图片成功：{}（{} 字节）", target, file.getSize());
        return UploadConfig.URL_PREFIX + "/" + monthDir + "/" + filename;
    }

    private String extensionOf(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

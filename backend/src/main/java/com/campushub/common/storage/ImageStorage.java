package com.campushub.common.storage;

import com.campushub.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * Web 可访问图床（与现有 LocalFsObjectStorage 不同：那个返回 file:// 不能被浏览器访问）。
 *
 * - 落盘：{baseDir}/ab/cd/abcd...ef.jpg （SHA-256 分桶，幂等去重）
 * - URL：/uploads/ab/cd/abcd...ef.jpg （Spring static resource handler 映射到 baseDir）
 *
 * 配套：
 *   - WebMvcConfig.addResourceHandlers 把 /uploads/** 映射到 file:./uploads/
 *   - UploadController POST /api/uploads → 调本类 put(file) → 返回 {url}
 *   - application.properties: spring.servlet.multipart.max-file-size=5MB
 *
 * 生产可替换为 OSS/Minio，但要保持「返回 HTTP URL，前端能 <img src> 直接拉」契约。
 */
@Component
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );

    private final Path baseDir;
    private final String urlPrefix;

    public ImageStorage(
            @Value("${campushub.upload.dir:./uploads}") String dir,
            @Value("${campushub.upload.url-prefix:/uploads}") String urlPrefix
    ) {
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建上传目录: " + baseDir, e);
        }
        log.info("ImageStorage initialized at {} (url prefix: {})", baseDir, this.urlPrefix);
    }

    /**
     * 上传一个图片文件 → 返回可被浏览器访问的 URL。
     *
     * 校验：非空、≤5MB、MIME 在白名单内。
     */
    public String put(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(4001, "上传文件为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BizException(4002, "文件超过 5MB 上限");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase())) {
            throw new BizException(4003, "仅支持 jpg/png/webp/gif 图片");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(5001, "读取上传内容失败");
        }

        String sha = sha256(bytes);
        String ext = inferExtension(contentType);
        String relativePath = sha.substring(0, 2) + "/" + sha.substring(2, 4) + "/" + sha + ext;
        Path target = baseDir.resolve(relativePath);

        try {
            Files.createDirectories(target.getParent());
            if (!Files.exists(target)) {
                Files.write(target, bytes);
            }
        } catch (IOException e) {
            throw new IllegalStateException("写入上传目录失败: " + target, e);
        }

        return urlPrefix + "/" + relativePath;
    }

    /** 暴露给 WebMvcConfig 配置 ResourceHandler 用。 */
    public Path getBaseDir() {
        return baseDir;
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String inferExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".bin";
        };
    }
}

package com.campushub.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * dev/test 对象存储：写到本地 tmp 目录，按 SHA-256 命名实现内容去重。
 *
 * 路径示例：{baseDir}/ab/cd/abcd...ef.jpg
 * URL：file://{absolutePath}（仅作占位；前端不应真去拉，业务表只存 sha256）
 *
 * 生产应换成 MinioObjectStorage 等远程实现（不破坏 ObjectStorage 接口契约）。
 */
@Component
@Profile("!prod")
public class LocalFsObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFsObjectStorage.class);

    private final Path baseDir;

    public LocalFsObjectStorage(
            @Value("${campushub.storage.local.dir:#{systemProperties['java.io.tmpdir']}/campushub-uploads}")
            String dir) {
        this.baseDir = Paths.get(dir);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建对象存储目录: " + baseDir, e);
        }
        log.info("LocalFsObjectStorage initialized at {}", baseDir);
    }

    @Override
    public PutResult put(byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("上传内容为空");
        }
        String sha = sha256(content);
        String ext = inferExtension(contentType);
        Path target = baseDir
                .resolve(sha.substring(0, 2))
                .resolve(sha.substring(2, 4))
                .resolve(sha + ext);
        try {
            Files.createDirectories(target.getParent());
            if (!Files.exists(target)) {
                Files.write(target, content);
            }
        } catch (IOException e) {
            throw new IllegalStateException("写入对象存储失败: " + target, e);
        }
        return new PutResult(sha, "file://" + target.toAbsolutePath());
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
        if (contentType == null) return ".bin";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".bin";
        };
    }
}

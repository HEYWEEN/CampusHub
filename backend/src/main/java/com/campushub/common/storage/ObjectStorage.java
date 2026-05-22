package com.campushub.common.storage;

/**
 * 对象存储抽象（学生证图、二手商品图等共用）。
 *
 * 实现：
 *   - LocalFsObjectStorage（dev/test）：写本地 tmp 目录，返回 file:// URL
 *   - 后续可加 MinioObjectStorage / OssObjectStorage（生产）
 *
 * 上传约定：
 *   - 内容做 SHA-256 → 同内容多次上传得到同一 URL（去重 + 幂等）
 *   - URL 是公开访问 URL（auth_verification 的图按 SHA 索引）
 */
public interface ObjectStorage {

    /**
     * 上传二进制内容。返回结果含 sha256 哈希 + 可访问 URL。
     *
     * @param content       字节内容
     * @param contentType   MIME（如 image/jpeg）；可空，用于推断扩展名
     */
    PutResult put(byte[] content, String contentType);

    record PutResult(String sha256, String url) {}
}

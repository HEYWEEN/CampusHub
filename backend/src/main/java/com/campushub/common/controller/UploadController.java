package com.campushub.common.controller;

import com.campushub.common.response.ApiResponse;
import com.campushub.common.storage.ImageStorage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用图片上传接口 — 给所有需要 image url 的业务场景共用：
 *   - 用户头像（PATCH /api/users/me/profile { avatarUrl }）
 *   - 任务凭证（task proof images）
 *   - 二手商品图（trade item images）
 *   - 学生证认证（auth verification attachments）
 *
 * 流程：前端选文件 → POST /api/uploads (multipart) → 拿到 url
 *      → 把 url 塞进对应业务的 JSON 字段（业务接口仍是 application/json）。
 *
 * 这样做的好处：所有业务接口保持纯 JSON，不用各自实现 multipart 处理；
 * 数据库 schema 也只存 VARCHAR URL，不用改字段。
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final ImageStorage storage;

    public UploadController(ImageStorage storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResultVO> upload(@RequestParam("file") MultipartFile file) {
        String url = storage.put(file);
        return ApiResponse.success(new UploadResultVO(url));
    }

    public record UploadResultVO(String url) {}
}

package com.campushub.task.dto;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /api/tasks/{taskId}/proof  请求体（JSON）。
 *
 * schema_audit A-8 修复：原本是 multipart，改成 JSON + 预上传 URL，与 trade/profile/verify 统一。
 */
public class TaskProofSubmitDTO {

    /** 图片 URL 列表（由前端预先调 /api/uploads 拿） */
    @Size(max = 9, message = "凭证图最多 9 张")
    private List<String> images = new ArrayList<>();

    /** 文字说明（可选） */
    @Size(max = 1000, message = "凭证说明不超过 1000 字")
    private String text;

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images == null ? new ArrayList<>() : images; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}

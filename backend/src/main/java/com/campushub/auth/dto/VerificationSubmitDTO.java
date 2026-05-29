package com.campushub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * POST /api/auth/verifications  学生证认证提交。
 *
 * 字段约束：
 *   - realName：2~30 个字符（涵盖中外文姓名）
 *   - studentNo：6~20 位数字/字母
 *   - idCard：可空；身份证 18 位（最后一位可能 X）
 *   - attachmentUrls：1~5 张图 URL（前端先调 POST /api/uploads 拿）
 *
 * schema_audit A-12 修复：原本接 Base64 数组（巨型 payload），改为 URL 数组与 trade/profile 统一。
 *
 * 服务端会做：AES-GCM 加密 realName / studentNo / idCard → 落库（不存明文）。
 */
public class VerificationSubmitDTO {

    @NotBlank
    @Size(min = 2, max = 30, message = "真实姓名长度需在 2-30 之间")
    private String realName;

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "学号格式不正确")
    private String studentNo;

    @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    @NotEmpty(message = "至少需要 1 张证件图")
    @Size(max = 5, message = "证件图最多 5 张")
    private List<@NotBlank String> attachmentUrls;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public List<String> getAttachmentUrls() { return attachmentUrls; }
    public void setAttachmentUrls(List<String> attachmentUrls) {
        this.attachmentUrls = attachmentUrls;
    }
}

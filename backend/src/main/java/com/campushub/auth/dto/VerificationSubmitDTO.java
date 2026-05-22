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
 *   - attachmentsBase64：1~5 张图，base64 编码（不含 data: 前缀）
 *
 * 服务端会做：AES-GCM 加密 → SHA-256 → 落库（不存明文）。
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
    private List<@NotBlank String> attachmentsBase64;

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public List<String> getAttachmentsBase64() { return attachmentsBase64; }
    public void setAttachmentsBase64(List<String> attachmentsBase64) {
        this.attachmentsBase64 = attachmentsBase64;
    }
}

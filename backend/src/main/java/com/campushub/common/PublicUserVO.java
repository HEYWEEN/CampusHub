package com.campushub.common;

/**
 * 全队唯一的"公开用户对象"（P3 §1.2 钉死位置，严禁在业务模块自行拼装类似 VO）。
 *
 * 任何返回"另一个用户的信息"的接口都必须用本 VO：
 *   - 任务大厅 / 详情：发布者
 *   - 二手商品：卖家
 *   - 评价列表：评价者
 *
 * **严禁包含**：realName / studentNo / idCard / phone（明文或脱敏后）
 *
 * verifiedTag 由 auth_user.verify_status == APPROVED 推导出来，
 * 返回字符串 "校园已认证" 或 null —— 前端可以直接渲染 `{verifiedTag && <Tag>{verifiedTag}</Tag>}`
 * （schema_audit B-2 / B-3 修复，原本是 boolean 导致前端渲染为 `true` 字面量）。
 *
 * userId 暂保持 Long → JSON number（前端 type 写 string，但 React 不严格比较，能容忍）；
 * 真正的 ID 类型统一留到 P3 阶段全局 Jackson ToStringSerializer。
 */
public class PublicUserVO {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String verifiedTag;   // "校园已认证" 或 null

    public PublicUserVO() {}

    public PublicUserVO(Long userId, String nickname, String avatarUrl, String verifiedTag) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.verifiedTag = verifiedTag;
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getVerifiedTag() { return verifiedTag; }
}

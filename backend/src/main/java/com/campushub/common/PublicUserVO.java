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
 * 让前端可以无视底层 enum 直接展示"已认证" / "未认证"小标签。
 */
public class PublicUserVO {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private boolean verifiedTag;

    public PublicUserVO() {}

    public PublicUserVO(Long userId, String nickname, String avatarUrl, boolean verifiedTag) {
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.verifiedTag = verifiedTag;
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isVerifiedTag() { return verifiedTag; }
}

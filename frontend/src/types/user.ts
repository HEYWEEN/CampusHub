/**
 * 用户相关 VO/DTO — 与后端 user/vo/* 对齐
 * ⚠️ PublicUserVO 是全局唯一公开用户对象。严禁出现 realName/studentNo/phone(明文)。
 */

export type VerifyStatus = 'guest' | 'pending' | 'approved' | 'rejected'

export interface PublicUserVO {
  userId: string
  nickname: string
  avatarUrl?: string | null
  verifiedTag?: '校园已认证' | null
}

// 三项隐私开关 — 与后端 UserMeVO / PrivacyPatchDTO 字段名严格对齐（扁平结构）
export interface PrivacySettings {
  hidePublishHistory: boolean    // 默认 true（隐藏发布历史）
  hideAcceptHistory: boolean     // 默认 true（隐藏接单记录）
  hideCourseReviews: boolean     // 默认 true（隐藏「收到的评价」=订单互评；字段名为历史遗留）
}

// UserMeVO 把 PrivacySettings 三项字段展平在顶层（对齐后端扁平结构）
export interface UserMeVO extends PublicUserVO, PrivacySettings {
  phoneMasked: string            // 138****1234 后端已脱敏
  verifyStatus: VerifyStatus
  dailyAcceptLimit: number       // 1~3，可调
  role: 'USER' | 'ADMIN' | 'SUPER_ADMIN'  // 管理端入口/守卫用；SUPER_ADMIN 可分派管理员
  joinedAt?: string              // 注册时间 ISO（个人主页「加入 N 天」用）
  hasPassword: boolean           // 是否已设密码 → 决定「设置密码」vs「修改密码」
}

export interface ProfileUpdateDTO {
  nickname?: string
  avatarUrl?: string
}

// GET /api/users/me/stats —— 本人个人主页统计（真实计数，对齐后端 MeStatsVO）
export interface MeStatsVO {
  publishedCount: number        // 我发布的任务
  acceptedCount: number         // 我接过的任务
  reviewsCount: number          // 我收到的评价
  publishedInProgress: number   // 我发布的、进行中
  acceptedInProgress: number    // 我接的、进行中
  goodRate: number | null       // 好评率 %；无有效评价时 null
}

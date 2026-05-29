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
  hideCourseReviews: boolean     // 默认 true（隐藏课程评价）
}

// UserMeVO 把 PrivacySettings 三项字段展平在顶层（对齐后端扁平结构）
export interface UserMeVO extends PublicUserVO, PrivacySettings {
  phoneMasked: string            // 138****1234 后端已脱敏
  verifyStatus: VerifyStatus
  dailyAcceptLimit: number       // 1~3，可调
}

export interface ProfileUpdateDTO {
  nickname?: string
  avatarUrl?: string
}

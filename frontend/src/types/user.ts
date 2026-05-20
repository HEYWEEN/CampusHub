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

export interface PrivacySettings {
  hidePublishHist: boolean       // 默认 true（隐藏发布历史）
  hideAcceptHist: boolean        // 默认 true（隐藏接单记录）
  hideCourseReviews: boolean     // 默认 true（隐藏课程评价）
  imOpen: boolean                // 是否接收私信
}

export interface UserMeVO extends PublicUserVO {
  phoneMasked: string            // 138****1234 后端已脱敏
  verifyStatus: VerifyStatus
  privacy: PrivacySettings
  dailyAcceptLimit: number       // 1~3，可调
}

export interface ProfileUpdateDTO {
  nickname?: string
  avatarUrl?: string
}

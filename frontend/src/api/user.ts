import { apiGet, apiPatch } from './client'
import { BizError } from '../types/api'
import type {
  PrivacySettings,
  ProfileUpdateDTO,
  PublicUserVO,
  UserMeVO,
} from '../types/user'
import {
  mockGetMe,
  mockGetPublicStats,
  mockGetPublicUser,
  mockUpdateAcceptLimit,
  mockUpdatePrivacy,
  mockUpdateProfile,
  type PublicUserStats,
} from './_mock'

async function withMock<T>(real: () => Promise<T>, mock: () => T): Promise<T> {
  if (!import.meta.env.DEV) return real()
  try {
    return await real()
  } catch (err) {
    if (err instanceof BizError && err.code !== 0 && err.code !== 404 && err.code < 500) throw err
    // eslint-disable-next-line no-console
    console.warn('[mock] user API 后端未响应，使用 mock')
    return mock()
  }
}

export const getMe = () =>
  withMock<UserMeVO>(
    () => apiGet('/api/users/me'),
    () => mockGetMe(),
  )

export const getPublicUser = (userId: string) =>
  withMock<PublicUserVO>(
    () => apiGet(`/api/users/${userId}/public`),
    () => mockGetPublicUser(userId),
  )

export const getPublicStats = (userId: string) =>
  withMock<PublicUserStats>(
    () => apiGet(`/api/users/${userId}/public/stats`),
    () => mockGetPublicStats(userId),
  )

export const updateProfile = (dto: ProfileUpdateDTO) =>
  withMock<UserMeVO>(
    () => apiPatch('/api/users/me/profile', dto),
    () => mockUpdateProfile(dto),
  )

export const updatePrivacy = (privacy: PrivacySettings) =>
  withMock<UserMeVO>(
    () => apiPatch('/api/users/me/privacy', privacy),
    () => mockUpdatePrivacy(privacy),
  )

export const updateAcceptLimit = (dailyAcceptLimit: number) =>
  withMock<UserMeVO>(
    () => apiPatch('/api/users/me/accept-limit', { dailyAcceptLimit }),
    () => mockUpdateAcceptLimit(dailyAcceptLimit),
  )

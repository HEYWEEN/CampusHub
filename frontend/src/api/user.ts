import { apiGet, apiPatch } from './client'
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
import { withMock } from './withMock'

export const getMe = () =>
  withMock<UserMeVO>(
    () => apiGet('/api/users/me'),
    () => mockGetMe(),
    'user',
  )

export const getPublicUser = (userId: string) =>
  withMock<PublicUserVO>(
    () => apiGet(`/api/users/${userId}/public`),
    () => mockGetPublicUser(userId),
    'user',
  )

export const getPublicStats = (userId: string) =>
  withMock<PublicUserStats>(
    () => apiGet(`/api/users/${userId}/public/stats`),
    () => mockGetPublicStats(userId),
    'user',
  )

export const updateProfile = (dto: ProfileUpdateDTO) =>
  withMock<UserMeVO>(
    () => apiPatch('/api/users/me/profile', dto),
    () => mockUpdateProfile(dto),
    'user',
  )

export const updatePrivacy = (privacy: Partial<PrivacySettings>) =>
  withMock<UserMeVO>(
    () => apiPatch('/api/users/me/privacy', privacy),
    () => mockUpdatePrivacy(privacy),
    'user',
  )

export const updateAcceptLimit = (dailyAcceptLimit: number) =>
  withMock<UserMeVO>(
    () => apiPatch('/api/users/me/accept-limit', { dailyAcceptLimit }),
    () => mockUpdateAcceptLimit(dailyAcceptLimit),
    'user',
  )

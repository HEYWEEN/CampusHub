import { apiGet, apiPost, apiPatch } from './client'
import { type PageResponse } from '../types/api'
import type {
  TeamApplicationVO,
  TeamRecruitCreateDTO,
  TeamRecruitVO,
  TeamSearchParams,
} from '../types/team'
import {
  mockApplyTeam,
  mockCloseRecruit,
  mockCreateRecruit,
  mockGetRecruit,
  mockListApplications,
  mockReviewApplication,
  mockSearchTeams,
} from './_mock'
import { withMock } from './withMock'

export const searchTeams = (params: TeamSearchParams) =>
  withMock<PageResponse<TeamRecruitVO>>(
    () => apiGet('/api/search/teams', params),
    () => mockSearchTeams(params),
    'team',
  )

export const getRecruit = (id: number | string) =>
  withMock<TeamRecruitVO>(
    () => apiGet(`/api/team/recruits/${id}`),
    () => mockGetRecruit(Number(id)),
    'team',
  )

export const createRecruit = (dto: TeamRecruitCreateDTO) =>
  withMock<TeamRecruitVO>(
    () => apiPost('/api/team/recruits', dto),
    () => mockCreateRecruit(dto),
    'team',
  )

export const applyTeam = (id: number | string, message: string) =>
  withMock<void>(
    () => apiPost(`/api/team/recruits/${id}/applications`, { message }),
    () => mockApplyTeam(),
    'team',
  )

export const listApplications = (id: number | string) =>
  withMock<TeamApplicationVO[]>(
    () => apiGet(`/api/team/recruits/${id}/applications`),
    () => mockListApplications(),
    'team',
  )

export const reviewApplication = (applicationId: number | string, approve: boolean) =>
  withMock<void>(
    () => apiPatch(`/api/team/applications/${applicationId}`, { approve }),
    () => mockReviewApplication(),
    'team',
  )

export const closeRecruit = (id: number | string) =>
  withMock<void>(
    () => apiPatch(`/api/team/recruits/${id}/close`),
    () => mockCloseRecruit(Number(id)),
    'team',
  )

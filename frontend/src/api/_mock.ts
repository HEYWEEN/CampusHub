/**
 * Dev-only mock data layer
 * — 后端 API 失败时回退到这里，让前端能独立开发 & 演示
 * — 仅 import.meta.env.DEV 时生效；生产构建后这些函数仍存在但不会被调用
 */

import type { PageResponse } from '../types/api'
import { BizError } from '../types/api'
import type {
  TaskCreateDTO,
  TaskDetailVO,
  TaskListItemVO,
  TaskSearchParams,
} from '../types/task'
import type { PublicUserVO } from '../types/user'

// ───── 假用户 ─────
export const MOCK_CURRENT_USER_ID = 'u1'

const users: Record<string, PublicUserVO> = {
  u1: { userId: 'u1', nickname: '张大锤', avatarUrl: null, verifiedTag: '校园已认证' },
  u2: { userId: 'u2', nickname: '李小冰', avatarUrl: null, verifiedTag: '校园已认证' },
  u3: { userId: 'u3', nickname: '王晓明', avatarUrl: null, verifiedTag: null },
  u4: { userId: 'u4', nickname: '陈学姐', avatarUrl: null, verifiedTag: '校园已认证' },
  u5: { userId: 'u5', nickname: '阿白', avatarUrl: null, verifiedTag: '校园已认证' },
}

function inMinutes(n: number) {
  return new Date(Date.now() + n * 60_000).toISOString()
}
function minutesAgo(n: number) {
  return new Date(Date.now() - n * 60_000).toISOString()
}

// ───── 12 条种子任务（涵盖各类型 + 各状态） ─────
const tasks: TaskDetailVO[] = [
  {
    taskId: 't101',
    taskType: 'ERRAND',
    title: '帮我从南苑食堂带一份黄焖鸡米饭',
    detail: '南苑食堂 2 楼黄焖鸡窗口，不要香菜，多加一份木耳。打包好送到计科 311 教室门口。',
    rewardPoint: 8,
    deadlineAt: inMinutes(45),
    status: 'PENDING_ACCEPT',
    publisher: users.u2,
    building: '计科楼',
    attachments: [],
    version: 0,
    createdAt: minutesAgo(12),
  },
  {
    taskId: 't102',
    taskType: 'ERRAND',
    title: '菜鸟驿站取快递（顺丰大件）',
    detail: '驿站在 16 号宿舍楼下，快递柜密码 8842。送到 14 号 412 房间。',
    rewardPoint: 5,
    deadlineAt: inMinutes(120),
    status: 'PENDING_ACCEPT',
    publisher: users.u3,
    building: '16 号楼',
    attachments: [],
    version: 0,
    createdAt: minutesAgo(35),
  },
  {
    taskId: 't103',
    taskType: 'TUTOR',
    title: '高数 II 期中冲刺辅导（连续 4 次课）',
    detail: '想找会高数的学长学姐，每次 1 小时，4 次包月。主要复习二重积分、级数。',
    rewardPoint: 200,
    deadlineAt: inMinutes(60 * 24 * 3),
    status: 'PENDING_ACCEPT',
    publisher: users.u4,
    building: '在线 / 线下面议',
    attachments: [],
    version: 0,
    createdAt: minutesAgo(60 * 2),
  },
  {
    taskId: 't104',
    taskType: 'MUTUAL_HELP',
    title: '有人去仙林校区吗？想搭车一起回去',
    detail: '今晚 8 点左右从鼓楼回仙林，可以分摊滴滴。学姐人很好聊得来。',
    rewardPoint: 15,
    deadlineAt: inMinutes(60 * 4),
    status: 'PENDING_ACCEPT',
    publisher: users.u5,
    building: '鼓楼 → 仙林',
    attachments: [],
    version: 0,
    createdAt: minutesAgo(20),
  },
  {
    taskId: 't105',
    taskType: 'ERRAND',
    title: '帮拍一张行政楼前夕阳的照片',
    detail: '今晚日落前帮我拍 2-3 张行政楼的夕阳全景，发到 IM。',
    rewardPoint: 12,
    deadlineAt: inMinutes(90),
    status: 'IN_PROGRESS',
    publisher: users.u2,
    acceptor: users.u3,
    building: '行政楼',
    attachments: [],
    version: 1,
    createdAt: minutesAgo(60),
  },
  {
    taskId: 't106',
    taskType: 'ERRAND',
    title: '帮买一份阿姨包子（要韭菜的）',
    detail: '早餐摊在西门，3 个韭菜包，1 杯豆浆。',
    rewardPoint: 5,
    deadlineAt: minutesAgo(30),
    status: 'WAIT_CONFIRM',
    publisher: users.u1,
    acceptor: users.u3,
    building: '西门',
    attachments: [],
    proofImages: ['/illustrations/coffee.png'],
    proofNote: '已送到，3 个包子 + 豆浆，请确认。',
    version: 2,
    createdAt: minutesAgo(95),
  },
  {
    taskId: 't107',
    taskType: 'TUTOR',
    title: '操作系统 Lab2 求救（缺页中断那一关）',
    detail: 'Lab2 第三个测试用例怎么都过不了，怀疑是 LRU 实现有 bug，求大佬带飞 1 小时。',
    rewardPoint: 60,
    deadlineAt: inMinutes(60 * 12),
    status: 'COMPLETED',
    publisher: users.u4,
    acceptor: users.u1,
    building: '在线',
    attachments: [],
    version: 4,
    createdAt: minutesAgo(60 * 26),
  },
  {
    taskId: 't108',
    taskType: 'MUTUAL_HELP',
    title: '室友的猫不见了，求大家帮看下 5 栋附近',
    detail: '橘色狸花，戴红色项圈，名字叫"豆瓣"。看到请联系。重金致谢！',
    rewardPoint: 100,
    deadlineAt: inMinutes(60 * 48),
    status: 'PENDING_ACCEPT',
    publisher: users.u5,
    building: '5 栋宿舍楼附近',
    attachments: [],
    version: 0,
    createdAt: minutesAgo(15),
  },
  {
    taskId: 't109',
    taskType: 'ERRAND',
    title: '占座 · 图书馆 4 楼 A 区靠窗',
    detail: '需要占两个连座，明早 8:30 - 11:00 用。',
    rewardPoint: 6,
    deadlineAt: inMinutes(60 * 14),
    status: 'PENDING_ACCEPT',
    publisher: users.u1,
    building: '图书馆',
    attachments: [],
    version: 0,
    createdAt: minutesAgo(5),
  },
  {
    taskId: 't110',
    taskType: 'TUTOR',
    title: '计组实验 5 求助（Verilog ALU 设计）',
    detail: '加法器和移位器写完了，乘法那块一直时序不对，求大佬指点。',
    rewardPoint: 80,
    deadlineAt: inMinutes(60 * 36),
    status: 'CANCELED',
    publisher: users.u3,
    building: '在线',
    attachments: [],
    version: 1,
    createdAt: minutesAgo(60 * 8),
  },
  {
    taskId: 't111',
    taskType: 'ERRAND',
    title: '打印店取一份 30 页论文',
    detail: '北门打印店，单号 P-8841，付过款了直接取就行。',
    rewardPoint: 4,
    deadlineAt: minutesAgo(60),
    status: 'EXPIRED',
    publisher: users.u4,
    building: '北门打印店',
    attachments: [],
    version: 1,
    createdAt: minutesAgo(60 * 5),
  },
  {
    taskId: 't112',
    taskType: 'MUTUAL_HELP',
    title: '周末晨跑搭子（紫金山）',
    detail: '想找一两个稳定的跑友，每周六早上紫金山。配速 5:30 左右。',
    rewardPoint: 0,
    deadlineAt: inMinutes(60 * 60),
    status: 'PENDING_ACCEPT',
    publisher: users.u5,
    building: '紫金山北门',
    attachments: [],
    version: 0,
    createdAt: minutesAgo(60),
  },
]

function pageOf<T>(items: T[], page: number, size: number): PageResponse<T> {
  const start = (page - 1) * size
  return {
    items: items.slice(start, start + size),
    total: items.length,
    page,
    size,
  }
}

export function mockSearchTasks(params: TaskSearchParams): PageResponse<TaskListItemVO> {
  const { page = 1, size = 12, taskType, status, q } = params
  let filtered = [...tasks]
  if (taskType) filtered = filtered.filter((t) => t.taskType === taskType)
  if (status) filtered = filtered.filter((t) => t.status === status)
  if (q) {
    const kw = q.toLowerCase()
    filtered = filtered.filter(
      (t) => t.title.toLowerCase().includes(kw) || t.detail.toLowerCase().includes(kw),
    )
  }
  // 默认按创建时间倒序
  filtered.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  return pageOf(filtered, page, size)
}

export function mockGetTask(id: string): TaskDetailVO {
  const t = tasks.find((x) => x.taskId === id)
  if (!t) throw new BizError(404, '任务不存在')
  return { ...t }
}

export function mockCreateTask(dto: TaskCreateDTO, userId = MOCK_CURRENT_USER_ID): string {
  const taskId = 't' + Math.floor(Math.random() * 9000 + 1000)
  const newTask: TaskDetailVO = {
    taskId,
    taskType: dto.taskType,
    title: dto.title,
    detail: dto.detail,
    rewardPoint: dto.rewardPoint,
    deadlineAt: dto.deadlineAt,
    building: dto.building,
    status: 'PENDING_ACCEPT',
    publisher: users[userId] ?? users.u1,
    attachments: [],
    version: 0,
    createdAt: new Date().toISOString(),
  }
  tasks.unshift(newTask)
  return taskId
}

export function mockAcceptTask(id: string, userId = MOCK_CURRENT_USER_ID): void {
  const t = tasks.find((x) => x.taskId === id)
  if (!t) throw new BizError(404, '任务不存在')
  if (t.status !== 'PENDING_ACCEPT') throw new BizError(409, '任务已无法接单')
  if (t.publisher.userId === userId) throw new BizError(400, '不能接自己发布的任务')
  t.status = 'IN_PROGRESS'
  t.acceptor = users[userId] ?? users.u1
  t.version += 1
}

export function mockSubmitProof(id: string, images: string[], note: string): void {
  const t = tasks.find((x) => x.taskId === id)
  if (!t) throw new BizError(404, '任务不存在')
  if (t.status !== 'IN_PROGRESS') throw new BizError(409, '当前状态无法上传凭证')
  t.status = 'WAIT_CONFIRM'
  t.proofImages = images
  t.proofNote = note
  t.version += 1
}

export function mockConfirmTask(id: string): void {
  const t = tasks.find((x) => x.taskId === id)
  if (!t) throw new BizError(404, '任务不存在')
  if (t.status !== 'WAIT_CONFIRM') throw new BizError(409, '当前状态无法确认')
  t.status = 'COMPLETED'
  t.version += 1
}

export function mockCancelTask(id: string): void {
  const t = tasks.find((x) => x.taskId === id)
  if (!t) throw new BizError(404, '任务不存在')
  if (t.status === 'COMPLETED') throw new BizError(409, '已完成的任务无法取消')
  t.status = 'CANCELED'
  t.version += 1
}

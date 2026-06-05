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
import type { MeStatsVO, PrivacySettings, ProfileUpdateDTO, PublicUserVO, UserMeVO } from '../types/user'
import type { CreditAppealVO, CreditMeVO, CreditRecord, NotifyMessageVO, ReceivedReviewVO } from '../types/credit'
import type { AdminUserVO, AdminVerificationVO } from '../types/admin'
import type {
  TradeItemCreateDTO,
  TradeItemVO,
  TradeSearchParams,
  TradeOrderVO,
  TradeOfferVO,
} from '../types/trade'
import type {
  TeamApplicationVO,
  TeamRecruitCreateDTO,
  TeamRecruitVO,
  TeamSearchParams,
} from '../types/team'
import type { ImConversationVO, ImMessageType, ImMessageVO } from '../types/im'
import type { ReportCaseVO, ReportCreateDTO } from '../types/report'
import type { AgentChatResponse, AgentHistory } from '../types/agent'

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
    remark: '南苑食堂 2 楼黄焖鸡窗口，不要香菜，多加一份木耳。打包好送到计科 311 教室门口。',
    rewardPoint: 8,
    deadlineAt: inMinutes(45),
    pickupHint: '现场告知',
    status: 'PENDING_ACCEPT',
    publisher: users.u2,
    deliveryBuilding: '计科楼',
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(12),
  },
  {
    taskId: 't102',
    taskType: 'ERRAND',
    title: '菜鸟驿站取快递（顺丰大件）',
    remark: '驿站在 16 号宿舍楼下，快递柜密码 8842。送到 14 号 412 房间。',
    rewardPoint: 5,
    deadlineAt: inMinutes(120),
    pickupHint: '现场告知',
    status: 'PENDING_ACCEPT',
    publisher: users.u3,
    deliveryBuilding: '16 号楼',
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(35),
  },
  {
    taskId: 't103',
    taskType: 'TUTOR',
    title: '高数 II 期中冲刺辅导（连续 4 次课）',
    remark: '想找会高数的学长学姐，每次 1 小时，4 次包月。主要复习二重积分、级数。',
    rewardPoint: 200,
    deadlineAt: inMinutes(60 * 24 * 3),
    pickupHint: '现场告知',
    status: 'PENDING_ACCEPT',
    publisher: users.u4,
    deliveryBuilding: '在线 / 线下面议',
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(60 * 2),
  },
  {
    taskId: 't104',
    taskType: 'MUTUAL_HELP',
    title: '有人去仙林校区吗？想搭车一起回去',
    remark: '今晚 8 点左右从鼓楼回仙林，可以分摊滴滴。学姐人很好聊得来。',
    rewardPoint: 15,
    deadlineAt: inMinutes(60 * 4),
    pickupHint: '现场告知',
    status: 'PENDING_ACCEPT',
    publisher: users.u5,
    deliveryBuilding: '鼓楼 → 仙林',
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(20),
  },
  {
    taskId: 't105',
    taskType: 'ERRAND',
    title: '帮拍一张行政楼前夕阳的照片',
    remark: '今晚日落前帮我拍 2-3 张行政楼的夕阳全景，发到 IM。',
    rewardPoint: 12,
    deadlineAt: inMinutes(90),
    pickupHint: '现场告知',
    status: 'IN_PROGRESS',
    publisher: users.u2,
    assignee: users.u3,
    deliveryBuilding: '行政楼',
    attachmentUrls: [],
    version: 1,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(60),
  },
  {
    taskId: 't106',
    taskType: 'ERRAND',
    title: '帮买一份阿姨包子（要韭菜的）',
    remark: '早餐摊在西门，3 个韭菜包，1 杯豆浆。',
    rewardPoint: 5,
    deadlineAt: minutesAgo(30),
    pickupHint: '现场告知',
    status: 'WAIT_CONFIRM',
    publisher: users.u1,
    assignee: users.u3,
    deliveryBuilding: '西门',
    attachmentUrls: [],
    version: 2,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(95),
  },
  {
    taskId: 't107',
    taskType: 'TUTOR',
    title: '操作系统 Lab2 求救（缺页中断那一关）',
    remark: 'Lab2 第三个测试用例怎么都过不了，怀疑是 LRU 实现有 bug，求大佬带飞 1 小时。',
    rewardPoint: 60,
    deadlineAt: inMinutes(60 * 12),
    pickupHint: '现场告知',
    status: 'COMPLETED',
    publisher: users.u4,
    assignee: users.u1,
    deliveryBuilding: '在线',
    attachmentUrls: [],
    version: 4,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(60 * 26),
  },
  {
    taskId: 't108',
    taskType: 'MUTUAL_HELP',
    title: '室友的猫不见了，求大家帮看下 5 栋附近',
    remark: '橘色狸花，戴红色项圈，名字叫"豆瓣"。看到请联系。重金致谢！',
    rewardPoint: 100,
    deadlineAt: inMinutes(60 * 48),
    pickupHint: '现场告知',
    status: 'PENDING_ACCEPT',
    publisher: users.u5,
    deliveryBuilding: '5 栋宿舍楼附近',
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(15),
  },
  {
    taskId: 't109',
    taskType: 'ERRAND',
    title: '占座 · 图书馆 4 楼 A 区靠窗',
    remark: '需要占两个连座，明早 8:30 - 11:00 用。',
    rewardPoint: 6,
    deadlineAt: inMinutes(60 * 14),
    pickupHint: '现场告知',
    status: 'PENDING_ACCEPT',
    publisher: users.u1,
    deliveryBuilding: '图书馆',
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(5),
  },
  {
    taskId: 't110',
    taskType: 'TUTOR',
    title: '计组实验 5 求助（Verilog ALU 设计）',
    remark: '加法器和移位器写完了，乘法那块一直时序不对，求大佬指点。',
    rewardPoint: 80,
    deadlineAt: inMinutes(60 * 36),
    pickupHint: '现场告知',
    status: 'CANCELED',
    publisher: users.u3,
    deliveryBuilding: '在线',
    attachmentUrls: [],
    version: 1,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(60 * 8),
  },
  {
    taskId: 't111',
    taskType: 'ERRAND',
    title: '打印店取一份 30 页论文',
    remark: '北门打印店，单号 P-8841，付过款了直接取就行。',
    rewardPoint: 4,
    deadlineAt: minutesAgo(60),
    pickupHint: '现场告知',
    status: 'EXPIRED',
    publisher: users.u4,
    deliveryBuilding: '北门打印店',
    attachmentUrls: [],
    version: 1,
    canAccept: false,
    isPublisher: false,
    createdAt: minutesAgo(60 * 5),
  },
  {
    taskId: 't112',
    taskType: 'MUTUAL_HELP',
    title: '周末晨跑搭子（紫金山）',
    remark: '想找一两个稳定的跑友，每周六早上紫金山。配速 5:30 左右。',
    rewardPoint: 0,
    deadlineAt: inMinutes(60 * 60),
    pickupHint: '现场告知',
    status: 'PENDING_ACCEPT',
    publisher: users.u5,
    deliveryBuilding: '紫金山北门',
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: false,
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
  const { page = 1, size = 12, taskType, status, q, publisherId, assigneeId } = params
  let filtered = [...tasks]
  if (taskType) filtered = filtered.filter((t) => t.taskType === taskType)
  if (status) filtered = filtered.filter((t) => t.status === status)
  if (publisherId != null) filtered = filtered.filter((t) => t.publisher.userId === String(publisherId))
  // mock 列表项不带接单者信息，「我接的」离线下统一空态
  if (assigneeId != null) filtered = []
  if (q) {
    const kw = q.toLowerCase()
    filtered = filtered.filter(
      (t) => t.title.toLowerCase().includes(kw) || (t.remark || "").toLowerCase().includes(kw),
    )
  }
  // 默认按创建时间倒序
  filtered.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  return pageOf(filtered, page, size)
}

export function mockRecommendedTasks(limit = 8): TaskListItemVO[] {
  // 离线 mock：取待接单任务，按悬赏高 + 较新粗排，返回前 limit 条
  return [...tasks]
    .filter((t) => t.status === 'PENDING_ACCEPT')
    .sort((a, b) => b.rewardPoint - a.rewardPoint || b.createdAt.localeCompare(a.createdAt))
    .slice(0, limit)
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
    remark: dto.remark || '',
    rewardPoint: dto.rewardPoint,
    deadlineAt: dto.deadlineAt,
    pickupHint: dto.pickupHint,
    deliveryBuilding: dto.deliveryBuilding,
    status: 'PENDING_ACCEPT',
    publisher: users[userId] ?? users.u1,
    attachmentUrls: [],
    version: 0,
    canAccept: false,
    isPublisher: true,
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
  t.assignee = users[userId] ?? users.u1
  t.version += 1
}

export function mockSubmitProof(id: string, images: string[]): void {
  const t = tasks.find((x) => x.taskId === id)
  if (!t) throw new BizError(404, '任务不存在')
  if (t.status !== 'IN_PROGRESS') throw new BizError(409, '当前状态无法上传凭证')
  t.status = 'WAIT_CONFIRM'
  // 凭证图直接进 attachmentUrls；note 在后端 mock 层暂时丢弃（后端 task proof 是 multipart text+files 单独传）
  t.attachmentUrls = [...(t.attachmentUrls || []), ...images]
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

// ─────────────────────────────────────────────
// 用户 / 信用 mock（D 阶段）
// ─────────────────────────────────────────────

const me: UserMeVO = {
  userId: MOCK_CURRENT_USER_ID,
  nickname: '张大锤',
  avatarUrl: null,
  verifiedTag: '校园已认证',
  phoneMasked: '138****8842',
  verifyStatus: 'approved',
  hidePublishHistory: true,
  hideAcceptHistory: true,
  hideCourseReviews: true,
  dailyAcceptLimit: 2,
  role: 'ADMIN',   // mock 下给 admin，方便 DEV 跳过登录后体验管理后台
  joinedAt: '2024-01-26T08:00:00Z',
  hasPassword: true,
}

const credit: CreditMeVO = {
  userId: MOCK_CURRENT_USER_ID,
  creditScore: 85,
  pointBalance: 320,
  pointFrozen: 50,
  dailyAcceptLimit: 2,
  canPublish: true,
  canAccept: true,
}

export function mockGetMe(): UserMeVO {
  return { ...me }
}

export function mockGetCredit(): CreditMeVO {
  return { ...credit }
}

export function mockUpdateProfile(dto: ProfileUpdateDTO): UserMeVO {
  if (dto.nickname !== undefined) {
    me.nickname = dto.nickname
    users[me.userId] = { ...users[me.userId], nickname: dto.nickname }
  }
  if (dto.avatarUrl !== undefined) {
    me.avatarUrl = dto.avatarUrl
    users[me.userId] = { ...users[me.userId], avatarUrl: dto.avatarUrl }
  }
  return mockGetMe()
}

export function mockUpdatePrivacy(privacy: Partial<PrivacySettings>): UserMeVO {
  Object.assign(me, privacy)
  return mockGetMe()
}

export function mockUpdateAcceptLimit(limit: number): UserMeVO {
  if (limit < 1 || limit > 3) throw new BizError(400, '接单上限只能 1-3')
  me.dailyAcceptLimit = limit
  credit.dailyAcceptLimit = limit
  return mockGetMe()
}

export function mockGetPublicUser(userId: string): PublicUserVO {
  if (userId === me.userId) {
    return {
      userId: me.userId,
      nickname: me.nickname,
      avatarUrl: me.avatarUrl,
      verifiedTag: me.verifiedTag,
    }
  }
  const u = users[userId]
  if (!u) throw new BizError(404, '用户不存在')
  return { ...u }
}

// ─────────────────────────────────────────────
// 二手 trade mock（E 阶段）
// ─────────────────────────────────────────────

const items: TradeItemVO[] = [
  {
    id: 101,
    title: 'Kindle Paperwhite 4（无划痕）',
    pricePoint: 380,
    description: '九成新，2022 年买的，平时只在宿舍用。保护套和数据线都送。\n仙林面交，可以试用一会儿再决定。',
    imageUrls: ['/illustrations/reading.png'],
    pickupLocationType: 'BUILDING_RANGE',
    pickupLocationDetail: '仙林 14 号楼',
    status: 'ON_SALE',
    seller: users.u2,
    createdAt: minutesAgo(60),
  },
  {
    id: 102,
    title: '计算机组成原理（清华版）+ 配套实验书',
    pricePoint: 25,
    description: '考完研留下的，笔记不多，正文很干净。两本一起出。',
    imageUrls: ['/illustrations/focused.png'],
    pickupLocationType: 'EXACT_DORM',
    pickupLocationDetail: '16 号楼 412',
    status: 'ON_SALE',
    seller: users.u4,
    createdAt: minutesAgo(60 * 3),
  },
  {
    id: 103,
    title: 'iPad mini 6 64G WiFi 版',
    pricePoint: 2100,
    description: '紫色，1 年质保还剩 4 个月。平时就看 PDF 和记笔记，无任何磕碰。',
    imageUrls: ['/illustrations/coffee.png'],
    pickupLocationType: 'MEETUP',
    pickupLocationDetail: '南门 / 西门均可',
    status: 'ON_SALE',
    seller: users.u3,
    createdAt: minutesAgo(60 * 8),
  },
  {
    id: 104,
    title: '宿舍小电饭锅（毕业转手）',
    pricePoint: 80,
    description: '1.6L，正好够 2 人吃。煮饭炖汤煮粥都可以。明年 6 月毕业现在低价出。',
    imageUrls: ['/illustrations/plant.png'],
    pickupLocationType: 'EXACT_DORM',
    pickupLocationDetail: '5 号楼 308',
    status: 'IN_TRADE',
    seller: users.u5,
    createdAt: minutesAgo(60 * 24),
  },
  {
    id: 105,
    title: '高数考研全程班教材（张宇 + 李永乐）',
    pricePoint: 40,
    description: '基础到强化，含视频网盘账号（剩余 6 个月）',
    imageUrls: ['/illustrations/free-time.png'],
    pickupLocationType: 'BUILDING_RANGE',
    pickupLocationDetail: '仙林任意宿舍楼',
    status: 'ON_SALE',
    seller: users.u4,
    createdAt: minutesAgo(60 * 12),
  },
  {
    id: 106,
    title: 'Sony WH-1000XM4 降噪耳机',
    pricePoint: 1500,
    description: '银色，1 年内的，无瑕疵。包装盒和配件齐全。',
    imageUrls: ['/illustrations/reflecting.png'],
    pickupLocationType: 'MEETUP',
    pickupLocationDetail: '校内任意地点',
    status: 'ON_SALE',
    seller: users.u1,
    createdAt: minutesAgo(30),
  },
  {
    id: 107,
    title: '折叠椅（带杯架 · 露营也能用）',
    pricePoint: 35,
    description: '阳台坐久了腰疼买的，结果没用几次。颜色军绿。',
    imageUrls: ['/illustrations/catching-up.png'],
    pickupLocationType: 'EXACT_DORM',
    pickupLocationDetail: '14 号楼 305',
    status: 'OFF_SALE',
    seller: users.u2,
    createdAt: minutesAgo(60 * 60),
  },
  {
    id: 108,
    title: '手冲咖啡套装（V60 滤杯 + 手冲壶 + 电子秤）',
    pricePoint: 120,
    description: '一整套出，新买的电子秤还没拆封。本来想晨起来一杯，结果还是奶茶香。',
    imageUrls: ['/illustrations/coffee.png'],
    pickupLocationType: 'BUILDING_RANGE',
    pickupLocationDetail: '鼓楼校区',
    status: 'ON_SALE',
    seller: users.u3,
    createdAt: minutesAgo(60 * 5),
  },
]

export function mockSearchItems(params: TradeSearchParams): PageResponse<TradeItemVO> {
  const { page = 1, size = 12, status, q } = params
  let filtered = [...items]
  if (status) filtered = filtered.filter((i) => i.status === status)
  if (q) {
    const kw = q.toLowerCase()
    filtered = filtered.filter(
      (i) => i.title.toLowerCase().includes(kw) || i.description.toLowerCase().includes(kw),
    )
  }
  filtered.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  return pageOf(filtered, page, size)
}

export function mockGetItem(id: string): TradeItemVO {
  const idNum = Number(id)
  const it = items.find((x) => x.id === idNum)
  if (!it) throw new BizError(404, '商品不存在')
  return { ...it, imageUrls: [...it.imageUrls] }
}

// ─────────────────────────────────────────────
// 通知 + 积分流水 mock（F 阶段）
// ─────────────────────────────────────────────

const notifications: NotifyMessageVO[] = [
  { id: 'n1', type: 'TASK_ACCEPTED', title: '你的任务已被接单', body: '王晓明 接了你的「占座 · 图书馆 4 楼 A 区」', createdAt: minutesAgo(2), bizId: 't109' },
  { id: 'n2', type: 'CREDIT_SETTLE', title: '积分到账',         body: '辅导任务完成，60 积分已结算到你的账户',    createdAt: minutesAgo(45), bizId: 't107', readAt: minutesAgo(20) },
  { id: 'n3', type: 'TASK_PROOF',    title: '凭证已上传',        body: '王晓明 上传了任务「带早餐」的完成凭证，请尽快确认',  createdAt: minutesAgo(60 * 2), bizId: 't106' },
  { id: 'n4', type: 'REVIEW',        title: '你收到一条评价',    body: '李小冰 给你打了 5 星好评："沟通顺畅，速度快"', createdAt: minutesAgo(60 * 5), bizId: 't107', readAt: minutesAgo(60 * 4) },
  { id: 'n5', type: 'SYSTEM',        title: '欢迎来到 CampusHub', body: '完成首次学生证认证可领 50 启动积分。',         createdAt: minutesAgo(60 * 24), readAt: minutesAgo(60 * 20) },
  { id: 'n6', type: 'TASK_REMINDER', title: '任务即将超时',      body: '你接的「拍夕阳」任务还有 10 分钟到期',          createdAt: minutesAgo(8), bizId: 't105' },
  { id: 'n7', type: 'CREDIT_FREEZE', title: '积分已冻结',         body: '你发布了任务，10 积分作为悬赏被冻结',          createdAt: minutesAgo(12), bizId: 't109', readAt: minutesAgo(5) },
]

const records: CreditRecord[] = [
  { id: 'r1', direction: 'FREEZE',   delta: -10,  reasonCode: 'TASK_PUBLISH',  bizId: 't109', createdAt: minutesAgo(12) },
  { id: 'r2', direction: 'SETTLE',   delta: 60,   reasonCode: 'TASK_COMPLETE', bizId: 't107', createdAt: minutesAgo(60 * 5) },
  { id: 'r3', direction: 'UNFREEZE', delta: 8,    reasonCode: 'TASK_CANCEL',   bizId: 't110', createdAt: minutesAgo(60 * 8) },
  { id: 'r4', direction: 'DEDUCT',   delta: -5,   reasonCode: 'BAD_REVIEW',    createdAt: minutesAgo(60 * 24 * 2) },
  { id: 'r5', direction: 'SETTLE',   delta: 12,   reasonCode: 'TASK_COMPLETE', bizId: 't050', createdAt: minutesAgo(60 * 30) },
  { id: 'r6', direction: 'FREEZE',   delta: -50,  reasonCode: 'TASK_PUBLISH',  bizId: 't048', createdAt: minutesAgo(60 * 48) },
  { id: 'r7', direction: 'SETTLE',   delta: 200,  reasonCode: 'TUTOR_COMPLETE',bizId: 't040', createdAt: minutesAgo(60 * 72) },
]

export function mockListNotifications(filter?: 'all' | 'unread' | 'read'): NotifyMessageVO[] {
  const list = [...notifications].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  if (filter === 'unread') return list.filter((n) => !n.readAt)
  if (filter === 'read')   return list.filter((n) => !!n.readAt)
  return list
}

export function mockUnreadCount(): number {
  return notifications.filter((n) => !n.readAt).length
}

export function mockMarkRead(id: string): void {
  const n = notifications.find((x) => x.id === id)
  if (n && !n.readAt) n.readAt = new Date().toISOString()
}

export function mockMarkAllRead(): void {
  const now = new Date().toISOString()
  notifications.forEach((n) => { if (!n.readAt) n.readAt = now })
}

export function mockListCreditRecords(): CreditRecord[] {
  return [...records].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

export function mockCreateItem(dto: TradeItemCreateDTO, userId = MOCK_CURRENT_USER_ID): TradeItemVO {
  const id = Math.floor(Math.random() * 9000 + 1000)
  const newItem: TradeItemVO = {
    id,
    title: dto.title,
    pricePoint: dto.pricePoint,
    description: dto.description ?? '',
    imageUrls: dto.imageUrls.length > 0 ? [...dto.imageUrls] : ['/illustrations/free-time.png'],
    pickupLocationType: dto.pickupLocationType,
    pickupLocationDetail: dto.pickupLocationDetail ?? '',
    status: 'ON_SALE',
    seller: users[userId] ?? users.u1,
    createdAt: new Date().toISOString(),
  }
  items.unshift(newItem)
  return newItem
}

// ─────────────────────────────────────────────
// 二手 订单 / 砍价（离线 mock — 真后端为主路径，此处仅 dev fallback）
// ─────────────────────────────────────────────

const orders: TradeOrderVO[] = []
const offers: TradeOfferVO[] = []
const MOCK_ME_NUM = 1
const MOCK_PEER_NUM = 2

export function mockCreateOrder(itemId: number, negotiatedPricePoint: number): TradeOrderVO {
  const order: TradeOrderVO = {
    id: Math.floor(Math.random() * 9000 + 1000),
    itemId,
    buyerId: MOCK_ME_NUM,
    sellerId: MOCK_PEER_NUM,
    status: 'IN_TRADE',
    negotiatedPricePoint,
    freezePoint: negotiatedPricePoint,
    buyerConfirmed: false,
    sellerConfirmed: false,
    createdAt: new Date().toISOString(),
  }
  orders.unshift(order)
  return order
}

export function mockGetOrder(orderId: number): TradeOrderVO {
  const o = orders.find((x) => x.id === orderId)
  if (!o) throw new BizError(404, '订单不存在')
  return o
}

export function mockConfirmOrder(orderId: number): TradeOrderVO {
  const o = mockGetOrder(orderId)
  o.buyerConfirmed = true
  o.status = o.sellerConfirmed ? 'COMPLETED' : 'BUYER_CONFIRMED'
  return o
}

export function mockCancelOrder(orderId: number): TradeOrderVO {
  const o = mockGetOrder(orderId)
  o.status = 'CANCELED'
  return o
}

export function mockListMyOrders(): TradeOrderVO[] {
  return [...orders]
}

function mockOfferShell(itemId: number, pricePoint: number): TradeOfferVO {
  const item = items.find((x) => x.id === itemId)
  return {
    id: Math.floor(Math.random() * 9000 + 1000),
    itemId,
    itemTitle: item?.title ?? `商品 #${itemId}`,
    itemPricePoint: item?.pricePoint ?? pricePoint,
    buyer: users[MOCK_CURRENT_USER_ID] ?? users.u1,
    seller: item?.seller ?? users.u2,
    pricePoint,
    status: 'PENDING',
    awaitingRole: 'SELLER',
    isBuyer: true,
    myTurn: false,
    orderId: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  }
}

export function mockCreateOffer(itemId: number, pricePoint: number): TradeOfferVO {
  const offer = mockOfferShell(itemId, pricePoint)
  offers.unshift(offer)
  return offer
}

function findOffer(offerId: number): TradeOfferVO {
  const o = offers.find((x) => x.id === offerId)
  if (!o) throw new BizError(404, '报价不存在')
  return o
}

export function mockCounterOffer(offerId: number, pricePoint: number): TradeOfferVO {
  const o = findOffer(offerId)
  o.pricePoint = pricePoint
  o.awaitingRole = o.awaitingRole === 'BUYER' ? 'SELLER' : 'BUYER'
  o.myTurn = o.isBuyer ? o.awaitingRole === 'BUYER' : o.awaitingRole === 'SELLER'
  o.updatedAt = new Date().toISOString()
  return o
}

export function mockAcceptOffer(offerId: number): TradeOfferVO {
  const o = findOffer(offerId)
  o.status = 'ACCEPTED'
  o.myTurn = false
  const order = mockCreateOrder(o.itemId, o.pricePoint)
  o.orderId = order.id
  return o
}

export function mockRejectOffer(offerId: number): TradeOfferVO {
  const o = findOffer(offerId)
  o.status = 'REJECTED'
  o.myTurn = false
  return o
}

export function mockCancelOffer(offerId: number): TradeOfferVO {
  const o = findOffer(offerId)
  o.status = 'CANCELED'
  o.myTurn = false
  return o
}

export function mockListMyOffers(): TradeOfferVO[] {
  return [...offers]
}

// ─────────────────────────────────────────────
// PublicUserStats（user 模块已用）
// ─────────────────────────────────────────────

/** 当前用户对某用户的 "公开 stats"（受隐私开关影响） */
export interface PublicUserStats {
  user: PublicUserVO
  publishedCount: number | null   // null = 该项被隐藏
  acceptedCount: number | null
  reviewsCount: number | null
}

export function mockGetPublicStats(userId: string): PublicUserStats {
  const user = mockGetPublicUser(userId)
  // 「查看公开主页」= 预览外人所见，自己看自己也受隐私开关约束（与后端一致）。
  if (userId === me.userId) {
    return {
      user,
      publishedCount: me.hidePublishHistory ? null
        : tasks.filter((t) => t.publisher.userId === userId).length,
      acceptedCount: me.hideAcceptHistory ? null
        : tasks.filter((t) => t.assignee?.userId === userId).length,
      reviewsCount: me.hideCourseReviews ? null : 12,
    }
  }
  // mock：假设其他用户隐私设置与 me 相同（全部 hide）
  return {
    user,
    publishedCount: null,
    acceptedCount: null,
    reviewsCount: null,
  }
}

/** 本人个人主页统计（真实计数，不受隐私开关影响）。 */
export function mockGetMyStats(): MeStatsVO {
  const inProgress = (s: string) => s === 'IN_PROGRESS' || s === 'WAIT_CONFIRM'
  const mine = tasks.filter((t) => t.publisher.userId === me.userId)
  const taken = tasks.filter((t) => t.assignee?.userId === me.userId)
  return {
    publishedCount: mine.length,
    acceptedCount: taken.length,
    reviewsCount: 12,
    publishedInProgress: mine.filter((t) => inProgress(t.status)).length,
    acceptedInProgress: taken.filter((t) => inProgress(t.status)).length,
    goodRate: 92,
  }
}

// ───── team 组队 mock ─────
const teamRecruits: TeamRecruitVO[] = [
  {
    recruitId: 901, title: '数模国赛三缺一', description: '冲国一，缺一个会编程的队友，每周两次讨论。',
    skillTags: ['数学建模', 'Python', 'LaTeX'], totalSize: 3, currentSize: 2, status: 'RECRUITING',
    creator: users.u4, createdAt: minutesAgo(40), isCreator: false, myApplicationStatus: null,
  },
  {
    recruitId: 902, title: '软工二大作业组队', description: '校园互助平台方向，前后端都缺人。',
    skillTags: ['React', 'Spring Boot', 'MySQL'], totalSize: 5, currentSize: 3, status: 'RECRUITING',
    creator: users.u1, createdAt: minutesAgo(120), isCreator: true, myApplicationStatus: null,
  },
  {
    recruitId: 903, title: '校园马拉松 4×100 接力', description: '找三个能跑的，周末校运会。',
    skillTags: ['跑步', '4x100'], totalSize: 4, currentSize: 4, status: 'FULL',
    creator: users.u3, createdAt: minutesAgo(1440), isCreator: false, myApplicationStatus: 'APPROVED',
  },
  {
    recruitId: 904, title: '创新创业大赛找产品', description: '技术齐了，缺一个懂产品 / 商业的。',
    skillTags: ['产品', '商业计划书', 'PPT'], totalSize: 4, currentSize: 2, status: 'RECRUITING',
    creator: users.u5, createdAt: minutesAgo(300), isCreator: false, myApplicationStatus: 'PENDING',
  },
]

const teamApplications: TeamApplicationVO[] = [
  { applicationId: 7001, applicant: users.u2, creditScore: 96, message: '做过两次数模，会 Python', status: 'PENDING', createdAt: minutesAgo(30) },
  { applicationId: 7002, applicant: users.u3, creditScore: 88, message: '想试试，态度好', status: 'PENDING', createdAt: minutesAgo(15) },
  { applicationId: 7003, applicant: users.u5, creditScore: 100, message: '会 LaTeX 排版', status: 'APPROVED', createdAt: minutesAgo(200) },
]

export function mockSearchTeams(params: TeamSearchParams): PageResponse<TeamRecruitVO> {
  let list = teamRecruits
  if (params.status) list = list.filter((r) => r.status === params.status)
  if (params.tag) list = list.filter((r) => r.skillTags.some((t) => t.includes(params.tag!)))
  if (params.q) {
    const q = params.q
    list = list.filter((r) => r.title.includes(q) || (r.description ?? '').includes(q))
  }
  return { items: list, total: list.length, page: params.page ?? 1, size: params.size ?? 12 }
}

export function mockGetRecruit(id: number): TeamRecruitVO {
  const r = teamRecruits.find((x) => x.recruitId === id)
  if (!r) throw new BizError(404, '组队帖不存在', 404)
  return r
}

export function mockCreateRecruit(dto: TeamRecruitCreateDTO, userId = MOCK_CURRENT_USER_ID): TeamRecruitVO {
  const r: TeamRecruitVO = {
    recruitId: Math.floor(Math.random() * 100000) + 1000,
    title: dto.title, description: dto.description ?? null, skillTags: dto.skillTags,
    totalSize: dto.totalSize, currentSize: 1, status: 'RECRUITING',
    creator: users[userId] ?? users.u1, createdAt: new Date().toISOString(),
    isCreator: true, myApplicationStatus: null,
  }
  teamRecruits.unshift(r)
  return r
}

export function mockApplyTeam(): void {
  // no-op：mock 下申请即视为提交成功
}

export function mockListApplications(): TeamApplicationVO[] {
  return teamApplications
}

export function mockReviewApplication(): void {
  // no-op
}

// ───── im 私信 mock ─────
const imConversations: ImConversationVO[] = [
  { conversationId: 1, peer: users.u4, lastMessage: '好的，明天图书馆见～', lastContentType: 'TEXT', lastMsgAt: minutesAgo(8), unreadCount: 2 },
  { conversationId: 2, peer: users.u2, lastMessage: '[图片]', lastContentType: 'IMAGE', lastMsgAt: minutesAgo(120), unreadCount: 0 },
  { conversationId: 3, peer: users.u3, lastMessage: '任务已被接单，双方可在此沟通 🤝', lastContentType: 'SYSTEM', lastMsgAt: minutesAgo(1440), unreadCount: 0 },
]

const imMessages: Record<number, ImMessageVO[]> = {
  1: [
    { messageId: 11, senderId: 4, mine: false, contentType: 'SYSTEM', content: '任务已被接单，双方可在此沟通 🤝', createdAt: minutesAgo(60) },
    { messageId: 12, senderId: 1, mine: true, contentType: 'TEXT', content: '学姐你好，关于高数辅导', createdAt: minutesAgo(40) },
    { messageId: 13, senderId: 4, mine: false, contentType: 'TEXT', content: '可以呀，你想约什么时候？', createdAt: minutesAgo(30) },
    { messageId: 14, senderId: 1, mine: true, contentType: 'TEXT', content: '明天下午方便吗', createdAt: minutesAgo(12) },
    { messageId: 15, senderId: 4, mine: false, contentType: 'TEXT', content: '好的，明天图书馆见～', createdAt: minutesAgo(8) },
  ],
}

export function mockListConversations(): ImConversationVO[] {
  return imConversations
}

export function mockStartConversation(peerId: number): ImConversationVO {
  const found = imConversations.find((c) => c.peer.userId === String(peerId))
  if (found) return found
  const conv: ImConversationVO = {
    conversationId: Math.floor(Math.random() * 100000) + 100,
    peer: users[`u${peerId}`] ?? users.u2,
    lastMessage: null, lastContentType: null, lastMsgAt: new Date().toISOString(), unreadCount: 0,
  }
  imConversations.unshift(conv)
  return conv
}

export function mockGetMessages(conversationId: number): PageResponse<ImMessageVO> {
  const items = [...(imMessages[conversationId] ?? [])].reverse()  // 后端倒序返回
  return { items, total: items.length, page: 1, size: 30 }
}

export function mockSendMessage(content: string, contentType: ImMessageType): ImMessageVO {
  return {
    messageId: Math.floor(Math.random() * 100000) + 1000,
    senderId: 1, mine: true, contentType, content, createdAt: new Date().toISOString(),
  }
}

export function mockGetUnread(): { count: number } {
  return { count: imConversations.reduce((s, c) => s + c.unreadCount, 0) }
}

// ───── 信用申诉 mock ─────
const receivedReviews: ReceivedReviewVO[] = [
  { reviewId: 5001, taskId: 301, reviewer: users.u3, rating: 1, comment: '迟到又敷衍', voided: false, underAppeal: false, appealable: true, createdAt: minutesAgo(120) },
  { reviewId: 5002, taskId: 302, reviewer: users.u2, rating: 5, comment: '靠谱，准时', voided: false, underAppeal: false, appealable: false, createdAt: minutesAgo(2880) },
]
const myAppeals: CreditAppealVO[] = [
  { appealId: 6001, reviewId: 5003, reviewRating: 2, reviewComment: '态度一般', reason: '当时排队很久不是我的问题', evidenceUrls: [], status: 'PENDING', resolveNote: null, appellant: null, createdAt: minutesAgo(60) },
]
export function mockReceivedReviews(): ReceivedReviewVO[] { return receivedReviews }
export function mockListMyAppeals(): CreditAppealVO[] { return myAppeals }
export function mockSubmitAppeal(reviewId: number, reason: string): CreditAppealVO {
  const a: CreditAppealVO = { appealId: Math.floor(Math.random() * 100000), reviewId, reviewRating: 1, reviewComment: '迟到又敷衍', reason, evidenceUrls: [], status: 'PENDING', resolveNote: null, appellant: null, createdAt: new Date().toISOString() }
  myAppeals.unshift(a)
  return a
}

// ───── admin mock ─────
export function mockAdminVerifications(): AdminVerificationVO[] {
  return [
    { verificationId: 7001, userId: 4, realName: '李小冰', studentNo: 'MG2433001', idCard: null, attachmentUrls: ['/illustrations/nju-gate.png'], status: 'pending', createdAt: minutesAgo(30) },
    { verificationId: 7002, userId: 5, realName: '王晓明', studentNo: 'MG2433066', idCard: null, attachmentUrls: ['/illustrations/beidalou.png'], status: 'pending', createdAt: minutesAgo(90) },
  ]
}
export function mockAdminSearchUsers(q: string): AdminUserVO[] {
  const all: AdminUserVO[] = [
    { userId: 2, nickname: '李小冰', avatarUrl: null, verifyStatus: 'approved', banned: false, role: 'USER' },
    { userId: 3, nickname: '王晓明', avatarUrl: null, verifyStatus: 'guest', banned: false, role: 'USER' },
    { userId: 4, nickname: '阿白', avatarUrl: null, verifyStatus: 'approved', banned: true, role: 'USER' },
  ]
  return q ? all.filter((u) => u.nickname?.includes(q) || String(u.userId) === q) : all
}
export function mockAdminAppeals(): CreditAppealVO[] {
  return [
    { appealId: 6010, reviewId: 5101, reviewRating: 1, reviewComment: '没按时送达', reason: '驿站当时关门了，非我原因，有照片', evidenceUrls: ['/illustrations/coffee.png'], status: 'PENDING', resolveNote: null, appellant: users.u2, createdAt: minutesAgo(45) },
  ]
}

// ───── 举报 / 仲裁 mock ─────
const reportCases: ReportCaseVO[] = [
  {
    caseId: 7001, reporter: users.u2, targetType: 'USER', targetId: 3,
    reasonCategory: 'HARASSMENT', description: '私信骚扰，言语不当', evidenceUrls: ['/illustrations/coffee.png'],
    status: 'PENDING', decisionType: null, createdAt: minutesAgo(30),
  },
  {
    caseId: 7002, reporter: users.u3, targetType: 'TASK', targetId: 1010,
    reasonCategory: 'FRAUD', description: '任务描述与实际不符，疑似骗积分', evidenceUrls: [],
    status: 'PENDING', decisionType: null, createdAt: minutesAgo(120),
  },
]

export function mockAdminReports(): ReportCaseVO[] {
  return reportCases.filter((c) => c.status === 'PENDING')
}

export function mockMyReports(): ReportCaseVO[] {
  return reportCases.map((c) => ({ ...c, reporter: null }))
}

// ───── AI 助手 mock（后端未接/未配 key 时回退） ─────
export function mockAgentHistory(): AgentHistory {
  return { conversationId: null, messages: [] }
}

export function mockAgentChat(message: string): AgentChatResponse {
  const wantPost = /发布|发个|帮我发|发条|发布个/.test(message)
  if (wantPost) {
    return {
      conversationId: 1,
      reply: '（演示）我整理了一份草稿，点「去发布」确认或修改～',
      actions: [{
        type: 'task_draft',
        draft: {
          title: message.replace(/帮我|发布|发个/g, '').trim().slice(0, 20) || '新任务',
          taskType: 'ERRAND',
          rewardPoint: 5,
          deadlineIso: new Date(Date.now() + 86400000).toISOString(),
          deliveryBuilding: '紫金楼',
          remark: '',
        },
      }],
    }
  }
  return {
    conversationId: 1,
    reply: '（演示）帮你找到这些待接单的任务：',
    actions: [{ type: 'task_results', tasks: mockRecommendedTasks(4) }],
  }
}

export function mockSubmitReport(dto: ReportCreateDTO): ReportCaseVO {
  return {
    caseId: Math.floor(Math.random() * 9000 + 1000),
    reporter: null,
    targetType: dto.targetType,
    targetId: dto.targetId,
    reasonCategory: dto.reasonCategory,
    description: dto.description ?? null,
    evidenceUrls: dto.evidenceUrls ?? [],
    status: 'PENDING',
    decisionType: null,
    createdAt: new Date().toISOString(),
  }
}

// 默认值
export const DEFAULT_CITY_ID = 0
export const DEFAULT_DAYS = 3
export const DEFAULT_PAGE = 1
export const DEFAULT_PAGE_ZERO = 0
export const DEFAULT_PAGE_SIZE = 20
export const DEFAULT_PAGE_SIZE_SMALL = 10
export const DEFAULT_LIMIT = 10
export const DEFAULT_LIMIT_SMALL = 5
export const DEFAULT_RADIUS = 5000
export const DEFAULT_SYNC_MINUTES = 30

// 防抖延迟 (ms)
export const DEBOUNCE_DELAY = 300

// 文本截断
export const EXCERPT_MAX_LENGTH = 60
export const FEEDBACK_MAX_LENGTH = 500

// 拥挤度阈值 (对应后端crowdLevel: 1=空闲, 2=较少, 3=适中, 4=拥挤)
export const CROWD_LEVEL_HIGH = 4
export const CROWD_LEVEL_MEDIUM = 3
export const CROWD_LEVEL_LOW = 2

// 星级上限
export const MAX_RATING = 5

// 通用文本
export const TEXT = {
  NO_DESCRIPTION: '暂无描述',
  NO_RATING: '暂无评分',
  NO_CONTENT: '暂无内容',
  ANONYMOUS: '匿名',
  VIEW_ALL: '查看全部',
} as const

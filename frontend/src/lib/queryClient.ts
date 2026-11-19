import { QueryClient } from '@tanstack/react-query'
import { AuthError, isRequestTimeoutError } from './request'

/**
 * 全局 QueryClient：
 * - 服务端数据默认 30s 内复用缓存（切页不重拉）；
 * - 认证失败不重试（交给登录流程）；超时/网络错误最多重试 1 次；
 * - 其他错误重试 2 次。
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: (failureCount, error) => {
          if (error instanceof AuthError) return false
          if (isRequestTimeoutError(error)) return failureCount < 1
          return failureCount < 2
        },
        refetchOnWindowFocus: false,
      },
    },
  })
}

import type { AuthResponse, LoginRequest, RegisterRequest, User, ApiError } from '~/types/auth'
import type { DownloadJobDTO, DownloadRequest, Subtitle, CacheStats } from '~/types/streaming'

export const useApi = () => {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  const apiBase = config.public.apiBase

  const handleRequest = async <T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> => {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers,
    }

    // Add auth token if available
    if (authStore.token) {
      headers['Authorization'] = `Bearer ${authStore.token}`
    }

    try {
      const response = await fetch(`${apiBase}${endpoint}`, {
        ...options,
        headers,
      })

      if (!response.ok) {
        let errorMessage = `HTTP ${response.status}: ${response.statusText}`

        try {
          const errorData = await response.json()
          errorMessage = errorData.message || errorMessage
        } catch {
          // Ignore JSON parsing errors
        }

        throw new Error(errorMessage)
      }

      // Handle empty responses (e.g., 204 No Content)
      const contentType = response.headers.get('content-type')
      if (!contentType || !contentType.includes('application/json')) {
        return {} as T
      }

      return await response.json()
    } catch (error) {
      console.error('API request failed:', error)
      throw error
    }
  }

  return {
    // Generic HTTP methods
    get: <T>(endpoint: string, options: { params?: Record<string, any> } = {}) => {
      const url = new URL(`${apiBase}${endpoint}`, window.location.origin)
      if (options.params) {
        Object.entries(options.params).forEach(([key, value]) => {
          if (value !== undefined && value !== null && value !== '') {
            url.searchParams.append(key, String(value))
          }
        })
      }
      return handleRequest<T>(url.pathname + url.search, { method: 'GET' })
    },

    post: <T>(endpoint: string, data?: any) =>
      handleRequest<T>(endpoint, {
        method: 'POST',
        body: data ? JSON.stringify(data) : undefined,
      }),

    put: <T>(endpoint: string, data?: any) =>
      handleRequest<T>(endpoint, {
        method: 'PUT',
        body: data ? JSON.stringify(data) : undefined,
      }),

    delete: <T>(endpoint: string) =>
      handleRequest<T>(endpoint, { method: 'DELETE' }),

    // Auth endpoints
    auth: {
      login: (data: LoginRequest) =>
        handleRequest<AuthResponse>('/api/users/auth/login', {
          method: 'POST',
          body: JSON.stringify(data),
        }),

      register: (data: RegisterRequest) =>
        handleRequest<User>('/api/users/auth/register', {
          method: 'POST',
          body: JSON.stringify(data),
        }),

      me: () =>
        handleRequest<User>('/api/users/me'),

      logout: () =>
        handleRequest<void>('/api/users/auth/logout', {
          method: 'POST',
        }),
    },

    // Streaming endpoints
    streaming: {
      initiateDownload: (data: DownloadRequest) =>
        handleRequest<DownloadJobDTO>('/api/streaming/download', {
          method: 'POST',
          body: JSON.stringify(data),
        }),

      getJob: (jobId: string) =>
        handleRequest<DownloadJobDTO>(`/api/streaming/jobs/${jobId}`),

      checkReadiness: (jobId: string) =>
        handleRequest<{ jobId: string; ready: boolean; status: string; progress: number; filePath?: string }>(
          `/api/streaming/jobs/${jobId}/ready`
        ),

      getAllJobs: () =>
        handleRequest<DownloadJobDTO[]>('/api/streaming/jobs'),

      getUserJobs: (userId: string) =>
        handleRequest<DownloadJobDTO[]>(`/api/streaming/jobs/user/${userId}`),

      getSubtitles: (videoId: string) =>
        handleRequest<Subtitle[]>(`/api/streaming/subtitles/${videoId}`),

      getCacheStats: () =>
        handleRequest<CacheStats>('/api/streaming/cache/stats'),

      // These return URLs, not handled by JSON
      getVideoUrl: (jobId: string) => `${apiBase}/api/streaming/video/${jobId}`,
      getSubtitleUrl: (videoId: string, languageCode: string) =>
        `${apiBase}/api/streaming/subtitles/${videoId}/${languageCode}`,
    },
  }
}

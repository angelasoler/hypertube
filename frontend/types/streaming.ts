export interface DownloadJobDTO {
  id: string
  videoId: string
  torrentId: string
  userId: string
  status: 'PENDING' | 'DOWNLOADING' | 'CONVERTING' | 'COMPLETED' | 'FAILED'
  progress: number
  downloadSpeed?: number
  etaSeconds?: number
  filePath?: string
  errorMessage?: string
  createdAt: string
  startedAt?: string
  completedAt?: string
}

export interface DownloadRequest {
  videoId: string
  torrentId: string
  userId: string
  magnetLink?: string
  torrentUrl?: string
}

export interface Subtitle {
  id: string
  languageCode: string
  format: string
  available: boolean
}

export interface CacheStats {
  totalVideos: number
  expiredVideos: number
  totalSizeBytes: number
  totalSizeMB: number
  'storageTotal MB': number
  storageFreeMB: number
  storageUsedMB: number
  storageUsagePercent: string
}

<template>
  <div class="container mx-auto px-4 py-8 max-w-7xl">
    <!-- Loading State -->
    <div v-if="loading" class="text-center py-20">
      <div class="spinner-large"></div>
      <p class="mt-4 text-gray-400">Loading video...</p>
    </div>

    <!-- Not Found State -->
    <div v-else-if="!video" class="text-center py-20">
      <div class="text-6xl mb-4">🎬</div>
      <h2 class="text-2xl font-bold mb-2">Video Not Found</h2>
      <p class="text-gray-400 mb-6">The video you're looking for doesn't exist.</p>
      <NuxtLink to="/videos" class="btn btn-primary">
        Browse Videos
      </NuxtLink>
    </div>

    <!-- Video Content -->
    <div v-else>
      <!-- Breadcrumb -->
      <nav class="flex mb-6 text-sm">
        <NuxtLink to="/videos" class="text-blue-400 hover:underline">
          Videos
        </NuxtLink>
        <span class="mx-2 text-gray-500">/</span>
        <span class="text-gray-300">{{ video.title }}</span>
      </nav>

      <!-- Video Title and Metadata -->
      <div class="mb-8">
        <h1 class="text-4xl font-bold mb-3">{{ video.title }}</h1>
        <div class="flex flex-wrap gap-4 text-gray-300">
          <span class="flex items-center gap-2">
            📅 {{ video.year }}
          </span>
          <span v-if="video.rating" class="flex items-center gap-2">
            ⭐ {{ video.rating }}/10
          </span>
          <span v-if="video.duration" class="flex items-center gap-2">
            ⏱️ {{ video.duration }}
          </span>
          <span v-if="video.genre" class="flex items-center gap-2">
            🎭 {{ video.genre }}
          </span>
        </div>
        <p class="mt-4 text-gray-400 max-w-4xl">{{ video.description }}</p>
      </div>

      <!-- Download Progress (if not ready) -->
      <div v-if="downloadJob && !isReady" class="mb-8">
        <DownloadProgress
          :status="downloadJob.status"
          :progress="downloadJob.progress"
          :downloaded-bytes="downloadJob.downloadedBytes"
          :total-bytes="downloadJob.totalBytes"
          :download-speed="downloadJob.downloadSpeed"
          :eta="downloadJob.etaSeconds"
          :peers="downloadJob.peers"
          :current-phase="downloadJob.currentPhase"
          :error="downloadJob.errorMessage"
          @retry="retryDownload"
          @cancel="cancelDownload"
        />
      </div>

      <!-- Video Player (if ready) -->
      <div v-if="isReady && downloadJob" class="mb-8">
        <VideoPlayer
          :stream-url="videoUrl"
          :subtitles="subtitleTracks"
          :autoplay="true"
          @play="onVideoPlay"
          @pause="onVideoPause"
          @error="onVideoError"
        />
      </div>

      <!-- Start Download Button (if no job) -->
      <div v-if="!downloadJob" class="card p-8 text-center">
        <div class="text-4xl mb-4">▶️</div>
        <h3 class="text-xl font-semibold mb-2">Ready to watch?</h3>
        <p class="text-gray-400 mb-6">
          Click below to start downloading and streaming this video
        </p>
        <button
          @click="initiateDownload"
          :disabled="loading"
          class="btn btn-primary btn-lg"
        >
          <span v-if="loading">Starting...</span>
          <span v-else>🎬 Start Watching</span>
        </button>
      </div>

      <!-- Video Details Section -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mt-8">
        <!-- Main Info -->
        <div class="lg:col-span-2 card">
          <h3 class="text-xl font-semibold mb-4">About</h3>
          <div class="prose prose-invert max-w-none">
            <p class="text-gray-300">
              {{ video.longDescription || video.description }}
            </p>

            <div v-if="video.cast" class="mt-6">
              <h4 class="text-lg font-semibold mb-2">Cast</h4>
              <ul class="list-disc list-inside text-gray-300">
                <li v-for="actor in video.cast" :key="actor">{{ actor }}</li>
              </ul>
            </div>

            <div v-if="video.director" class="mt-4">
              <h4 class="text-lg font-semibold mb-2">Director</h4>
              <p class="text-gray-300">{{ video.director }}</p>
            </div>
          </div>
        </div>

        <!-- Technical Details -->
        <div class="card">
          <h3 class="text-xl font-semibold mb-4">Technical Info</h3>
          <dl class="space-y-3">
            <div>
              <dt class="text-sm text-gray-400">File Format</dt>
              <dd class="text-gray-200">MP4 (H.264)</dd>
            </div>
            <div v-if="downloadJob?.filePath">
              <dt class="text-sm text-gray-400">File Size</dt>
              <dd class="text-gray-200">{{ formatBytes(downloadJob.totalBytes) }}</dd>
            </div>
            <div>
              <dt class="text-sm text-gray-400">Subtitles</dt>
              <dd class="text-gray-200">
                {{ subtitleTracks.length > 0 ? subtitleTracks.map(s => s.label).join(', ') : 'None' }}
              </dd>
            </div>
            <div>
              <dt class="text-sm text-gray-400">Streaming</dt>
              <dd class="text-gray-200">Progressive (HTTP Range)</dd>
            </div>
            <div v-if="downloadJob">
              <dt class="text-sm text-gray-400">Job ID</dt>
              <dd class="text-gray-200 text-xs font-mono break-all">
                {{ downloadJob.id }}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <!-- Actions -->
      <div class="flex gap-4 mt-8">
        <NuxtLink to="/videos" class="btn btn-secondary">
          ← Back to Videos
        </NuxtLink>

        <button
          v-if="downloadJob && !isReady"
          @click="checkProgress"
          class="btn btn-secondary"
        >
          🔄 Refresh Status
        </button>

        <button
          v-if="isReady"
          @click="markAsWatched"
          class="btn btn-secondary"
        >
          ✓ Mark as Watched
        </button>
      </div>

      <!-- Error Alert -->
      <div v-if="error" class="alert alert-error mt-6">
        <span class="text-2xl">⚠️</span>
        <div>
          <h4 class="font-semibold">Error</h4>
          <p class="text-sm">{{ error }}</p>
        </div>
        <button @click="error = ''" class="btn-sm">
          ✕
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { testVideos } from '~/data/testVideos'
import type { TestVideo } from '~/data/testVideos'
import type { DownloadJobDTO, Subtitle } from '~/types/streaming'
import VideoPlayer from '~/components/VideoPlayer.vue'
import DownloadProgress from '~/components/DownloadProgress.vue'

definePageMeta({
  middleware: ['auth'],
})

const route = useRoute()
const api = useApi()
const authStore = useAuthStore()

const videoId = computed(() => route.params.id as string)
const video = ref<TestVideo | null>(null)
const downloadJob = ref<DownloadJobDTO | null>(null)
const subtitles = ref<Subtitle[]>([])
const isReady = ref(false)
const loading = ref(true)
const error = ref('')

let progressInterval: NodeJS.Timeout | null = null

const videoUrl = computed(() => {
  if (!downloadJob.value) return ''
  return api.streaming.getVideoUrl(downloadJob.value.id)
})

const subtitleTracks = computed(() => {
  return subtitles.value.map(sub => ({
    language: sub.languageCode,
    label: sub.languageCode.toUpperCase(),
    kind: 'subtitles',
    url: getSubtitleUrl(sub.languageCode),
    default: sub.languageCode === 'en',
  }))
})

// Load video data
onMounted(async () => {
  try {
    video.value = testVideos.find(v => v.id === videoId.value) || null

    if (!video.value) {
      error.value = 'Video not found'
    }
  } finally {
    loading.value = false
  }
})

// Cleanup on unmount
onUnmounted(() => {
  if (progressInterval) {
    clearInterval(progressInterval)
  }
})

const initiateDownload = async () => {
  if (!video.value || !authStore.user) return

  loading.value = true
  error.value = ''

  try {
    // Generate UUIDs (in production, these would come from API)
    const videoUUID = generateUUID()
    const torrentUUID = generateUUID()

    const request = {
      videoId: videoUUID,
      torrentId: torrentUUID,
      userId: authStore.user.id,
      magnetLink: video.value.magnetLink,
    }

    downloadJob.value = await api.streaming.initiateDownload(request)

    // Start monitoring progress
    startProgressMonitoring()

  } catch (err: any) {
    error.value = err.message || 'Failed to start download'
  } finally {
    loading.value = false
  }
}

const checkProgress = async () => {
  if (!downloadJob.value) return

  try {
    const response = await api.streaming.checkReadiness(downloadJob.value.id)

    // Update job data with more details
    downloadJob.value = {
      ...downloadJob.value,
      status: response.status as any,
      progress: response.progress,
      filePath: response.filePath,
      downloadedBytes: response.downloadedBytes || 0,
      totalBytes: response.totalBytes || 0,
      downloadSpeed: response.downloadSpeed || 0,
      etaSeconds: response.etaSeconds || 0,
      peers: response.peers,
      currentPhase: response.currentPhase,
      errorMessage: response.errorMessage,
    }

    // Check if ready for streaming
    if (response.ready && response.status === 'COMPLETED') {
      isReady.value = true

      // Stop polling
      if (progressInterval) {
        clearInterval(progressInterval)
        progressInterval = null
      }

      // Load subtitles
      await loadSubtitles()
    }

  } catch (err: any) {
    console.error('Failed to check progress:', err)
    // Don't show error to user for polling failures
  }
}

const startProgressMonitoring = () => {
  // Check immediately
  checkProgress()

  // Then poll every 2 seconds
  progressInterval = setInterval(() => {
    checkProgress()
  }, 2000)
}

const loadSubtitles = async () => {
  if (!downloadJob.value) return

  try {
    subtitles.value = await api.streaming.getSubtitles(downloadJob.value.videoId)
  } catch (err) {
    console.error('Failed to load subtitles:', err)
    // Not critical, continue without subtitles
  }
}

const getSubtitleUrl = (languageCode: string) => {
  if (!downloadJob.value) return ''
  return api.streaming.getSubtitleUrl(downloadJob.value.videoId, languageCode)
}

const retryDownload = async () => {
  if (!video.value) return

  // Reset state
  downloadJob.value = null
  isReady.value = false
  error.value = ''

  // Retry
  await initiateDownload()
}

const cancelDownload = async () => {
  if (!downloadJob.value) return

  try {
    // In production, call API to cancel download
    // await api.streaming.cancelDownload(downloadJob.value.id)

    // Reset state
    downloadJob.value = null
    isReady.value = false

    if (progressInterval) {
      clearInterval(progressInterval)
      progressInterval = null
    }
  } catch (err: any) {
    error.value = err.message || 'Failed to cancel download'
  }
}

const markAsWatched = () => {
  // In production, call API to mark video as watched
  console.log('Marking video as watched:', videoId.value)
}

const onVideoPlay = () => {
  console.log('Video started playing')
}

const onVideoPause = () => {
  console.log('Video paused')
}

const onVideoError = (errorMessage: string) => {
  error.value = `Video playback error: ${errorMessage}`
}

// Helper functions
const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

const formatBytes = (bytes: number): string => {
  if (!bytes) return 'N/A'
  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${units[i]}`
}
</script>

<style scoped>
.spinner-large {
  width: 60px;
  height: 60px;
  border: 4px solid #374151;
  border-top-color: #60a5fa;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.alert {
  display: flex;
  align-items: start;
  gap: 1rem;
  padding: 1rem;
  border-radius: 8px;
}

.alert-error {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #fca5a5;
}

.btn-lg {
  padding: 1rem 2rem;
  font-size: 1.125rem;
}

.prose {
  color: #d1d5db;
}

.prose h4 {
  color: #f3f4f6;
}
</style>

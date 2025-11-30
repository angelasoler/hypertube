<template>
  <div class="container mx-auto px-4 py-8">
    <div v-if="!video" class="text-center py-12">
      <p class="text-gray-400">Video not found</p>
      <NuxtLink to="/videos" class="btn btn-primary mt-4">
        Back to Videos
      </NuxtLink>
    </div>

    <div v-else>
      <!-- Video Title and Info -->
      <div class="mb-6">
        <h1 class="text-4xl font-bold mb-2">{{ video.title }}</h1>
        <p class="text-gray-400">{{ video.year }} • {{ video.description }}</p>
      </div>

      <!-- Download Progress -->
      <div v-if="!isReady && downloadJob" class="card mb-6">
        <h3 class="text-xl font-semibold mb-4">Preparing Video...</h3>

        <div class="space-y-4">
          <div>
            <div class="flex justify-between text-sm mb-2">
              <span class="text-gray-300">Status: {{ downloadJob.status }}</span>
              <span class="text-gray-300">{{ downloadJob.progress }}%</span>
            </div>
            <div class="w-full bg-gray-700 rounded-full h-4">
              <div
                class="bg-primary-600 h-4 rounded-full transition-all duration-300"
                :style="{ width: `${downloadJob.progress}%` }"
              ></div>
            </div>
          </div>

          <div v-if="downloadJob.downloadSpeed" class="text-sm text-gray-400">
            Download Speed: {{ formatSpeed(downloadJob.downloadSpeed) }}
          </div>

          <div v-if="downloadJob.etaSeconds" class="text-sm text-gray-400">
            ETA: {{ formatETA(downloadJob.etaSeconds) }}
          </div>

          <div v-if="downloadJob.errorMessage" class="p-4 bg-red-900/50 border border-red-500 rounded-lg">
            <p class="text-sm text-red-200">{{ downloadJob.errorMessage }}</p>
          </div>
        </div>
      </div>

      <!-- Video Player -->
      <div v-if="isReady && downloadJob" class="card mb-6">
        <video
          ref="videoPlayer"
          class="w-full rounded-lg"
          controls
          crossorigin="anonymous"
        >
          <source :src="videoUrl" type="video/mp4" />

          <!-- Subtitles -->
          <track
            v-for="subtitle in subtitles"
            :key="subtitle.languageCode"
            :label="subtitle.languageCode.toUpperCase()"
            :kind="'subtitles'"
            :srclang="subtitle.languageCode"
            :src="getSubtitleUrl(subtitle.languageCode)"
          />

          Your browser does not support the video tag.
        </video>

        <div class="mt-4 text-sm text-gray-400">
          <p>Video is streaming from torrent network</p>
          <p v-if="downloadJob.filePath" class="text-xs">Path: {{ downloadJob.filePath }}</p>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex gap-4">
        <NuxtLink to="/videos" class="btn btn-secondary">
          Back to Videos
        </NuxtLink>

        <button
          v-if="!downloadJob"
          @click="initiateDownload"
          :disabled="loading"
          class="btn btn-primary"
        >
          <span v-if="loading">Starting...</span>
          <span v-else>Start Download</span>
        </button>

        <button
          v-if="downloadJob && !isReady"
          @click="checkProgress"
          class="btn btn-secondary"
        >
          Refresh Status
        </button>
      </div>

      <!-- Error Display -->
      <div v-if="error" class="card mt-6 bg-red-900/20 border border-red-500">
        <h3 class="text-xl font-semibold mb-2 text-red-400">Error</h3>
        <p class="text-red-200">{{ error }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { testVideos } from '~/data/testVideos'
import type { TestVideo } from '~/data/testVideos'
import type { DownloadJobDTO, Subtitle } from '~/types/streaming'

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
const loading = ref(false)
const error = ref('')
const videoPlayer = ref<HTMLVideoElement | null>(null)

let progressInterval: NodeJS.Timeout | null = null

const videoUrl = computed(() => {
  if (!downloadJob.value) return ''
  return api.streaming.getVideoUrl(downloadJob.value.id)
})

// Find video from test data
onMounted(() => {
  video.value = testVideos.find(v => v.id === videoId.value) || null

  if (!video.value) {
    error.value = 'Video not found'
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
    // Generate UUIDs for video and torrent (in real app these would come from database)
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

    // Update job data
    downloadJob.value.status = response.status as any
    downloadJob.value.progress = response.progress
    downloadJob.value.filePath = response.filePath

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
    error.value = err.message || 'Failed to check progress'
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
  }
}

const getSubtitleUrl = (languageCode: string) => {
  if (!downloadJob.value) return ''
  return api.streaming.getSubtitleUrl(downloadJob.value.videoId, languageCode)
}

// Helper functions
const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

const formatSpeed = (bytesPerSecond: number): string => {
  const mbps = bytesPerSecond / (1024 * 1024)
  return `${mbps.toFixed(2)} MB/s`
}

const formatETA = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}m ${remainingSeconds}s`
}
</script>

<style scoped>
.line-clamp-3 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

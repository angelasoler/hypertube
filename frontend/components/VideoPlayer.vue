<template>
  <div class="video-player-container">
    <!-- Loading State -->
    <div v-if="loading" class="loading-overlay">
      <div class="spinner"></div>
      <p class="loading-text">{{ loadingMessage }}</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">⚠️</div>
      <h3 class="error-title">Playback Error</h3>
      <p class="error-message">{{ error }}</p>
      <button @click="retry" class="retry-button">
        🔄 Retry
      </button>
    </div>

    <!-- Video Player -->
    <div v-else class="player-wrapper">
      <video
        ref="videoElement"
        class="video-element"
        controls
        @loadedmetadata="onMetadataLoaded"
        @error="onVideoError"
        @play="onPlay"
        @pause="onPause"
        @timeupdate="onTimeUpdate"
      >
        <source :src="streamUrl" type="video/mp4" />
        <track
          v-for="subtitle in subtitles"
          :key="subtitle.language"
          :label="subtitle.label"
          :kind="subtitle.kind"
          :srclang="subtitle.language"
          :src="subtitle.url"
          :default="subtitle.default"
        />
        Your browser does not support the video tag.
      </video>

      <!-- Custom Controls Overlay (optional) -->
      <div v-if="showCustomControls" class="controls-overlay">
        <div class="progress-container">
          <div class="progress-bar" :style="{ width: `${progress}%` }"></div>
        </div>
        <div class="controls-buttons">
          <button @click="togglePlay" class="control-btn">
            {{ isPlaying ? '⏸️' : '▶️' }}
          </button>
          <span class="time-display">
            {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
          </span>
        </div>
      </div>
    </div>

    <!-- Video Info -->
    <div v-if="videoInfo" class="video-info">
      <div class="info-item">
        <span class="info-label">Duration:</span>
        <span class="info-value">{{ formatTime(videoInfo.duration) }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">Size:</span>
        <span class="info-value">{{ formatSize(videoInfo.size) }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">Quality:</span>
        <span class="info-value">{{ videoInfo.quality }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

interface Props {
  streamUrl: string
  subtitles?: Array<{
    language: string
    label: string
    kind: string
    url: string
    default?: boolean
  }>
  autoplay?: boolean
  showCustomControls?: boolean
}

interface VideoInfo {
  duration: number
  size: number
  quality: string
}

const props = withDefaults(defineProps<Props>(), {
  subtitles: () => [],
  autoplay: false,
  showCustomControls: false,
})

const emit = defineEmits<{
  (e: 'play'): void
  (e: 'pause'): void
  (e: 'ended'): void
  (e: 'error', error: string): void
}>()

const videoElement = ref<HTMLVideoElement | null>(null)
const loading = ref(true)
const loadingMessage = ref('Initializing player...')
const error = ref<string | null>(null)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const progress = ref(0)
const videoInfo = ref<VideoInfo | null>(null)

const onMetadataLoaded = () => {
  loading.value = false
  if (videoElement.value) {
    duration.value = videoElement.value.duration
    videoInfo.value = {
      duration: videoElement.value.duration,
      size: 0, // Would come from API
      quality: '1080p', // Would come from API
    }

    if (props.autoplay) {
      videoElement.value.play()
    }
  }
}

const onVideoError = (event: Event) => {
  const target = event.target as HTMLVideoElement
  let errorMessage = 'Failed to load video'

  if (target.error) {
    switch (target.error.code) {
      case MediaError.MEDIA_ERR_ABORTED:
        errorMessage = 'Video loading was aborted'
        break
      case MediaError.MEDIA_ERR_NETWORK:
        errorMessage = 'Network error occurred while loading video'
        break
      case MediaError.MEDIA_ERR_DECODE:
        errorMessage = 'Video decoding failed'
        break
      case MediaError.MEDIA_ERR_SRC_NOT_SUPPORTED:
        errorMessage = 'Video format not supported'
        break
    }
  }

  error.value = errorMessage
  loading.value = false
  emit('error', errorMessage)
}

const onPlay = () => {
  isPlaying.value = true
  emit('play')
}

const onPause = () => {
  isPlaying.value = false
  emit('pause')
}

const onTimeUpdate = () => {
  if (videoElement.value) {
    currentTime.value = videoElement.value.currentTime
    progress.value = (currentTime.value / duration.value) * 100
  }
}

const togglePlay = () => {
  if (videoElement.value) {
    if (isPlaying.value) {
      videoElement.value.pause()
    } else {
      videoElement.value.play()
    }
  }
}

const retry = () => {
  error.value = null
  loading.value = true
  loadingMessage.value = 'Retrying...'

  if (videoElement.value) {
    videoElement.value.load()
  }
}

const formatTime = (seconds: number): string => {
  if (!seconds || !isFinite(seconds)) return '0:00'

  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = Math.floor(seconds % 60)

  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  return `${minutes}:${secs.toString().padStart(2, '0')}`
}

const formatSize = (bytes: number): string => {
  if (bytes === 0) return 'N/A'

  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${units[i]}`
}

watch(() => props.streamUrl, () => {
  loading.value = true
  loadingMessage.value = 'Loading video...'
  error.value = null
})
</script>

<style scoped>
.video-player-container {
  position: relative;
  width: 100%;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
}

.loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background-color: #1a1a1a;
  color: #fff;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #333;
  border-top-color: #60a5fa;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  margin-top: 1rem;
  font-size: 1rem;
  color: #9ca3af;
}

.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 2rem;
  background-color: #1a1a1a;
  color: #fff;
}

.error-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.error-title {
  font-size: 1.5rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.error-message {
  color: #9ca3af;
  margin-bottom: 1.5rem;
  text-align: center;
}

.retry-button {
  padding: 0.75rem 2rem;
  background-color: #60a5fa;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-button:hover {
  background-color: #3b82f6;
}

.player-wrapper {
  position: relative;
  width: 100%;
}

.video-element {
  width: 100%;
  max-height: 80vh;
  display: block;
}

.controls-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.8), transparent);
  padding: 1rem;
  opacity: 0;
  transition: opacity 0.3s;
}

.player-wrapper:hover .controls-overlay {
  opacity: 1;
}

.progress-container {
  width: 100%;
  height: 4px;
  background-color: rgba(255,255,255,0.3);
  border-radius: 2px;
  margin-bottom: 0.5rem;
  cursor: pointer;
}

.progress-bar {
  height: 100%;
  background-color: #60a5fa;
  border-radius: 2px;
  transition: width 0.1s;
}

.controls-buttons {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.control-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0.25rem;
}

.time-display {
  color: #fff;
  font-size: 0.875rem;
  font-family: monospace;
}

.video-info {
  display: flex;
  gap: 2rem;
  padding: 1rem;
  background-color: #1f2937;
  color: #fff;
}

.info-item {
  display: flex;
  gap: 0.5rem;
}

.info-label {
  color: #9ca3af;
  font-weight: 600;
}

.info-value {
  color: #f3f4f6;
}
</style>

<template>
  <div class="download-progress">
    <div class="progress-header">
      <h3 class="progress-title">
        {{ statusIcon }} {{ statusText }}
      </h3>
      <span v-if="progress !== null" class="progress-percentage">
        {{ Math.round(progress) }}%
      </span>
    </div>

    <!-- Progress Bar -->
    <div class="progress-bar-container">
      <div
        class="progress-bar-fill"
        :class="progressBarClass"
        :style="{ width: `${progress || 0}%` }"
      ></div>
    </div>

    <!-- Details -->
    <div class="progress-details">
      <div v-if="downloadedBytes" class="detail-item">
        <span class="detail-label">Downloaded:</span>
        <span class="detail-value">{{ formatBytes(downloadedBytes) }} / {{ formatBytes(totalBytes) }}</span>
      </div>

      <div v-if="downloadSpeed" class="detail-item">
        <span class="detail-label">Speed:</span>
        <span class="detail-value">{{ formatBytes(downloadSpeed) }}/s</span>
      </div>

      <div v-if="eta" class="detail-item">
        <span class="detail-label">ETA:</span>
        <span class="detail-value">{{ formatDuration(eta) }}</span>
      </div>

      <div v-if="peers !== null" class="detail-item">
        <span class="detail-label">Peers:</span>
        <span class="detail-value">{{ peers }}</span>
      </div>

      <div v-if="currentPhase" class="detail-item">
        <span class="detail-label">Phase:</span>
        <span class="detail-value">{{ currentPhase }}</span>
      </div>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="error-message">
      <span class="error-icon">⚠️</span>
      <span>{{ error }}</span>
    </div>

    <!-- Action Buttons -->
    <div class="actions">
      <button
        v-if="status === 'FAILED' || status === 'CANCELLED'"
        @click="$emit('retry')"
        class="action-button retry-button"
      >
        🔄 Retry
      </button>

      <button
        v-if="status === 'IN_PROGRESS' || status === 'PENDING'"
        @click="$emit('cancel')"
        class="action-button cancel-button"
      >
        ✕ Cancel
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'CONVERTING'
  progress?: number | null
  downloadedBytes?: number
  totalBytes?: number
  downloadSpeed?: number
  eta?: number // seconds
  peers?: number
  currentPhase?: string
  error?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  progress: null,
  downloadedBytes: 0,
  totalBytes: 0,
  downloadSpeed: 0,
  eta: 0,
  peers: null,
  currentPhase: '',
  error: null,
})

defineEmits<{
  (e: 'retry'): void
  (e: 'cancel'): void
}>()

const statusText = computed(() => {
  switch (props.status) {
    case 'PENDING':
      return 'Pending...'
    case 'IN_PROGRESS':
      return 'Downloading...'
    case 'CONVERTING':
      return 'Converting...'
    case 'COMPLETED':
      return 'Ready to Watch'
    case 'FAILED':
      return 'Download Failed'
    case 'CANCELLED':
      return 'Cancelled'
    default:
      return 'Unknown'
  }
})

const statusIcon = computed(() => {
  switch (props.status) {
    case 'PENDING':
      return '⏳'
    case 'IN_PROGRESS':
      return '⬇️'
    case 'CONVERTING':
      return '🔄'
    case 'COMPLETED':
      return '✅'
    case 'FAILED':
      return '❌'
    case 'CANCELLED':
      return '⛔'
    default:
      return '❓'
  }
})

const progressBarClass = computed(() => {
  switch (props.status) {
    case 'IN_PROGRESS':
      return 'progress-bar-downloading'
    case 'CONVERTING':
      return 'progress-bar-converting'
    case 'COMPLETED':
      return 'progress-bar-completed'
    case 'FAILED':
      return 'progress-bar-failed'
    default:
      return 'progress-bar-pending'
  }
})

const formatBytes = (bytes: number): string => {
  if (!bytes || bytes === 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const k = 1024
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${units[i]}`
}

const formatDuration = (seconds: number): string => {
  if (!seconds || seconds === 0) return 'calculating...'
  if (seconds < 60) return `${Math.round(seconds)}s`

  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)

  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`
  }
  return `${minutes}m ${Math.round(seconds % 60)}s`
}
</script>

<style scoped>
.download-progress {
  background: linear-gradient(135deg, #1f2937 0%, #111827 100%);
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.progress-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #f3f4f6;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.progress-percentage {
  font-size: 1.5rem;
  font-weight: bold;
  color: #60a5fa;
  font-family: monospace;
}

.progress-bar-container {
  width: 100%;
  height: 12px;
  background-color: #374151;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 1rem;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.3s ease, background-color 0.3s ease;
}

.progress-bar-pending {
  background: linear-gradient(90deg, #6b7280, #9ca3af);
  animation: pulse 2s infinite;
}

.progress-bar-downloading {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
  animation: shimmer 2s infinite;
}

.progress-bar-converting {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
  animation: shimmer 2s infinite;
}

.progress-bar-completed {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.progress-bar-failed {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

@keyframes shimmer {
  0% {
    background-position: -1000px 0;
  }
  100% {
    background-position: 1000px 0;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.progress-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-label {
  font-size: 0.75rem;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-value {
  font-size: 0.875rem;
  color: #f3f4f6;
  font-weight: 500;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 6px;
  color: #fca5a5;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.error-icon {
  font-size: 1.25rem;
}

.actions {
  display: flex;
  gap: 0.75rem;
}

.action-button {
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.retry-button {
  background-color: #3b82f6;
  color: #fff;
}

.retry-button:hover {
  background-color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
}

.cancel-button {
  background-color: #6b7280;
  color: #fff;
}

.cancel-button:hover {
  background-color: #4b5563;
}
</style>

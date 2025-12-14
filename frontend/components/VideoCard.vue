<template>
  <div
    class="video-card group relative overflow-hidden rounded-lg transition-all duration-300 hover:scale-105 hover:shadow-2xl cursor-pointer"
    :class="{ 'watched': video.watched }"
    data-testid="video-card"
    @click="$emit('click', video)"
  >
    <!-- Watched Indicator -->
    <div v-if="video.watched" class="absolute top-2 right-2 z-10 bg-green-500 text-white px-2 py-1 rounded-full text-xs font-semibold flex items-center gap-1">
      <span>✓</span>
      <span>Watched</span>
    </div>

    <!-- Poster Image -->
    <div class="relative aspect-[2/3] overflow-hidden bg-gray-800">
      <img
        :src="video.posterUrl || '/placeholder-poster.jpg'"
        :alt="video.title"
        class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
        @error="handleImageError"
      />

      <!-- Overlay on Hover -->
      <div class="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300">
        <div class="absolute bottom-0 left-0 right-0 p-4 transform translate-y-full group-hover:translate-y-0 transition-transform duration-300">
          <button class="btn btn-primary btn-sm w-full">
            ▶ Play Now
          </button>
        </div>
      </div>

      <!-- Rating Badge -->
      <div v-if="video.imdbRating" class="absolute top-2 left-2 bg-yellow-500 text-black px-2 py-1 rounded font-bold text-sm flex items-center gap-1">
        <span>⭐</span>
        <span>{{ video.imdbRating }}</span>
      </div>
    </div>

    <!-- Video Info -->
    <div class="p-4 bg-gray-900">
      <h3 class="font-semibold text-lg mb-2 line-clamp-1" :title="video.title">
        {{ video.title }}
      </h3>

      <div class="flex items-center justify-between text-sm text-gray-400 mb-2">
        <span>{{ video.year }}</span>
        <span v-if="video.runtimeMinutes">{{ formatRuntime(video.runtimeMinutes) }}</span>
      </div>

      <!-- Genres -->
      <div v-if="video.genres && video.genres.length > 0" class="flex flex-wrap gap-1 mb-2">
        <span
          v-for="genre in video.genres.slice(0, 3)"
          :key="genre"
          class="px-2 py-1 bg-gray-800 text-gray-300 rounded text-xs"
        >
          {{ genre }}
        </span>
      </div>

      <!-- Synopsis -->
      <p class="text-sm text-gray-400 line-clamp-2" :title="video.synopsis">
        {{ video.synopsis || 'No synopsis available' }}
      </p>
    </div>

    <!-- Watched Overlay -->
    <div v-if="video.watched" class="absolute inset-0 bg-black bg-opacity-30 pointer-events-none"></div>
  </div>
</template>

<script setup lang="ts">
interface Video {
  id: string
  imdbId?: string
  title: string
  year?: number
  runtimeMinutes?: number
  synopsis?: string
  imdbRating?: number
  posterUrl?: string
  backdropUrl?: string
  language?: string
  genres?: string[]
  watched?: boolean
}

interface Props {
  video: Video
}

defineProps<Props>()
defineEmits<{
  click: [video: Video]
}>()

const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'https://via.placeholder.com/300x450/1f2937/9ca3af?text=No+Poster'
}

const formatRuntime = (minutes: number): string => {
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return hours > 0 ? `${hours}h ${mins}m` : `${mins}m`
}
</script>

<style scoped>
.video-card.watched {
  opacity: 0.85;
}

.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>

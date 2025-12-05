<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-4xl font-bold mb-8">Browse Videos</h1>

    <!-- Filters -->
    <VideoFilters :filters="filters" @update="handleFilterUpdate" />

    <!-- Loading State -->
    <div v-if="loading && videos.length === 0" class="flex justify-center items-center py-20">
      <div class="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-primary-500"></div>
    </div>

    <!-- Results Info -->
    <div v-if="!loading || videos.length > 0" class="flex justify-between items-center mb-6">
      <p class="text-gray-400">
        {{ total }} video{{ total !== 1 ? 's' : '' }} found
      </p>
      <p class="text-gray-500 text-sm">
        Page {{ currentPage + 1 }} of {{ totalPages }}
      </p>
    </div>

    <!-- Video Grid -->
    <div
      v-if="videos.length > 0"
      class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4 mb-8"
    >
      <VideoCard
        v-for="video in videos"
        :key="video.id"
        :video="video"
        @click="goToVideo"
      />
    </div>

    <!-- Empty State -->
    <div v-if="!loading && videos.length === 0" class="text-center py-20">
      <div class="text-6xl mb-4">🎬</div>
      <h3 class="text-2xl font-semibold mb-2">No videos found</h3>
      <p class="text-gray-400 mb-6">
        Try adjusting your filters or search query
      </p>
      <button class="btn btn-primary" @click="resetAndSearch">
        Clear Filters
      </button>
    </div>

    <!-- Infinite Scroll Trigger & Load More -->
    <div ref="infiniteScrollTrigger" class="py-8">
      <div v-if="hasMore && !loading" class="text-center">
        <button class="btn btn-secondary" @click="loadMore">
          Load More Videos
        </button>
      </div>
      <div v-if="loading && videos.length > 0" class="flex justify-center">
        <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary-500"></div>
      </div>
      <div v-if="!hasMore && videos.length > 0" class="text-center text-gray-500">
        <p>You've reached the end</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { VideoFilters } from '~/components/VideoFilters.vue'

definePageMeta({
  middleware: ['auth'],
})

const authStore = useAuthStore()
const api = useApi()

// State
const videos = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(0)
const totalPages = ref(0)
const total = ref(0)
const hasMore = ref(true)

const filters = ref<VideoFilters>({
  query: '',
  genre: '',
  minYear: undefined,
  maxYear: undefined,
  minRating: undefined,
  sortBy: 'createdAt',
  sortDirection: 'desc'
})

// Infinite scroll trigger element
const infiniteScrollTrigger = ref<HTMLElement | null>(null)

// Fetch videos
const fetchVideos = async (page: number = 0, append: boolean = false) => {
  if (loading.value) return

  loading.value = true
  try {
    const params: any = {
      page,
      size: 24, // Grid-friendly number (divisible by 2, 3, 4, 5, 6)
      sortBy: filters.value.sortBy || 'createdAt',
      sortDirection: filters.value.sortDirection || 'desc'
    }

    // Add optional filters
    if (filters.value.query) params.query = filters.value.query
    if (filters.value.genre) params.genre = filters.value.genre
    if (filters.value.minYear) params.minYear = filters.value.minYear
    if (filters.value.maxYear) params.maxYear = filters.value.maxYear
    if (filters.value.minRating) params.minRating = filters.value.minRating

    const response = await api.get('/videos/search', { params })

    if (append) {
      videos.value = [...videos.value, ...(response.data.content || [])]
    } else {
      videos.value = response.data.content || []
      // Scroll to top on new search
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    currentPage.value = response.data.page || 0
    totalPages.value = response.data.totalPages || 0
    total.value = response.data.total || 0
    hasMore.value = currentPage.value < totalPages.value - 1

  } catch (error: any) {
    console.error('Failed to fetch videos:', error)
    if (error.response?.status === 401) {
      authStore.logout()
      navigateTo('/login')
    }
  } finally {
    loading.value = false
  }
}

// Handle filter updates
const handleFilterUpdate = (newFilters: VideoFilters) => {
  filters.value = { ...newFilters }
  currentPage.value = 0
  fetchVideos(0, false)
}

// Load more (pagination)
const loadMore = () => {
  if (hasMore.value && !loading.value) {
    fetchVideos(currentPage.value + 1, true)
  }
}

// Reset filters and search
const resetAndSearch = () => {
  filters.value = {
    query: '',
    genre: '',
    minYear: undefined,
    maxYear: undefined,
    minRating: undefined,
    sortBy: 'createdAt',
    sortDirection: 'desc'
  }
  currentPage.value = 0
  fetchVideos(0, false)
}

// Navigate to video
const goToVideo = (video: any) => {
  navigateTo(`/watch/${video.id}`)
}

// Infinite scroll observer
let observer: IntersectionObserver | null = null

onMounted(() => {
  // Initial load
  fetchVideos(0, false)

  // Setup infinite scroll observer
  if (infiniteScrollTrigger.value) {
    observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (entry.isIntersecting && hasMore.value && !loading.value) {
          loadMore()
        }
      },
      {
        rootMargin: '100px', // Start loading 100px before reaching the element
      }
    )
    observer.observe(infiniteScrollTrigger.value)
  }
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})
</script>

<style scoped>
/* Smooth transitions for grid items */
.grid > * {
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

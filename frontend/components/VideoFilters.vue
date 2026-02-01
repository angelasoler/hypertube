<template>
  <div class="filters-panel bg-gray-900 rounded-lg p-6 mb-6 shadow-lg">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
      <!-- Search Input -->
      <div class="col-span-full lg:col-span-2">
        <label class="block text-sm font-medium text-gray-400 mb-2">Search</label>
        <input
          v-model="localFilters.query"
          type="text"
          placeholder="Search by title..."
          class="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          @keyup.enter="applyFilters"
        />
      </div>

      <!-- Genre Filter -->
      <div>
        <label class="block text-sm font-medium text-gray-400 mb-2">Genre</label>
        <select
          v-model="localFilters.genre"
          class="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
          @change="applyFilters"
        >
          <option value="">All Genres</option>
          <option v-for="genre in genres" :key="genre" :value="genre">
            {{ genre }}
          </option>
        </select>
      </div>

      <!-- Sort By -->
      <div>
        <label class="block text-sm font-medium text-gray-400 mb-2">Sort By</label>
        <select
          v-model="localFilters.sortBy"
          class="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
          @change="applyFilters"
        >
          <option value="createdAt">Latest Added</option>
          <option value="title">Title</option>
          <option value="year">Year</option>
          <option value="imdbRating">Rating</option>
        </select>
      </div>
    </div>

    <!-- Advanced Filters (Collapsible) -->
    <div class="border-t border-gray-800 pt-4">
      <button
        class="flex items-center gap-2 text-sm text-gray-400 hover:text-white transition-colors mb-4"
        @click="showAdvanced = !showAdvanced"
      >
        <span>{{ showAdvanced ? '▼' : '▶' }}</span>
        <span>Advanced Filters</span>
      </button>

      <div v-if="showAdvanced" class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- Year Range -->
        <div>
          <label class="block text-sm font-medium text-gray-400 mb-2">Min Year</label>
          <input
            v-model.number="localFilters.minYear"
            type="number"
            min="1900"
            :max="new Date().getFullYear()"
            placeholder="e.g., 2000"
            class="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-400 mb-2">Max Year</label>
          <input
            v-model.number="localFilters.maxYear"
            type="number"
            min="1900"
            :max="new Date().getFullYear()"
            placeholder="e.g., 2024"
            class="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>

        <!-- Min Rating -->
        <div>
          <label class="block text-sm font-medium text-gray-400 mb-2">Min Rating</label>
          <input
            v-model.number="localFilters.minRating"
            type="number"
            min="0"
            max="10"
            step="0.1"
            placeholder="e.g., 7.0"
            class="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </div>

      <!-- Filter Actions -->
      <div class="flex gap-4 mt-4">
        <button
          class="btn btn-primary flex-1"
          @click="applyFilters"
        >
          Apply Filters
        </button>
        <button
          class="btn btn-secondary"
          @click="resetFilters"
        >
          Reset
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
export interface VideoFilters {
  query?: string
  genre?: string
  minYear?: number
  maxYear?: number
  minRating?: number
  sortBy?: string
  sortDirection?: string
}

interface Props {
  filters: VideoFilters
}

const props = defineProps<Props>()
const emit = defineEmits<{
  update: [filters: VideoFilters]
}>()

const showAdvanced = ref(false)
const localFilters = ref<VideoFilters>({ ...props.filters })

// Common genres in movies
const genres = [
  'Action',
  'Adventure',
  'Animation',
  'Comedy',
  'Crime',
  'Documentary',
  'Drama',
  'Family',
  'Fantasy',
  'Horror',
  'Music',
  'Mystery',
  'Romance',
  'Science Fiction',
  'Thriller',
  'War',
  'Western'
]

const applyFilters = () => {
  emit('update', { ...localFilters.value })
}

const resetFilters = () => {
  localFilters.value = {
    query: '',
    genre: '',
    minYear: undefined,
    maxYear: undefined,
    minRating: undefined,
    sortBy: 'createdAt',
    sortDirection: 'desc'
  }
  applyFilters()
}

// Watch for external filter changes
watch(() => props.filters, (newFilters) => {
  localFilters.value = { ...newFilters }
}, { deep: true })
</script>

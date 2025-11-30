<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-4xl font-bold mb-8">Browse Videos</h1>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="video in videos"
        :key="video.id"
        class="card cursor-pointer hover:ring-2 hover:ring-primary-500 transition-all"
        @click="selectVideo(video)"
      >
        <img
          :src="video.thumbnail"
          :alt="video.title"
          class="w-full h-48 object-cover rounded-lg mb-4"
        />

        <h3 class="text-xl font-semibold mb-2">{{ video.title }}</h3>

        <p class="text-gray-400 text-sm mb-2">{{ video.year }}</p>

        <p class="text-gray-300 text-sm line-clamp-3">
          {{ video.description }}
        </p>

        <button class="btn btn-primary w-full mt-4">
          Watch Now
        </button>
      </div>
    </div>

    <div v-if="videos.length === 0" class="text-center py-12">
      <p class="text-gray-400">No videos available</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { testVideos } from '~/data/testVideos'
import type { TestVideo } from '~/data/testVideos'

definePageMeta({
  middleware: ['auth'],
})

const videos = ref<TestVideo[]>(testVideos)

const selectVideo = (video: TestVideo) => {
  navigateTo(`/watch/${video.id}`)
}
</script>

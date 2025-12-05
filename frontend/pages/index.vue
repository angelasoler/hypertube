<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-4xl font-bold mb-8">Welcome to HyperTube</h1>

    <div class="card mb-8 bg-gradient-to-r from-blue-900 to-purple-900 border-2 border-blue-500">
      <h2 class="text-3xl font-bold mb-4">✨ Video Streaming is Live!</h2>
      <p class="text-gray-200 mb-6 text-lg">
        Try our test video to see the complete streaming workflow in action!
      </p>
      <NuxtLink to="/watch/test-video" class="btn btn-primary btn-lg inline-flex items-center gap-2">
        <span>▶️</span>
        <span>Watch Test Video</span>
      </NuxtLink>
      <p class="text-gray-400 text-sm mt-4">
        Logged in as: <span class="text-primary-400">{{ authStore.user?.username }}</span>
      </p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="card">
        <h3 class="text-xl font-semibold mb-2">🎬 Browse Videos</h3>
        <p class="text-gray-400 mb-4">Search, filter, and explore our video library</p>
        <NuxtLink to="/browse" class="btn btn-secondary btn-sm">
          Browse Library →
        </NuxtLink>
      </div>

      <div class="card">
        <h3 class="text-xl font-semibold mb-2">📝 Subtitles</h3>
        <p class="text-gray-400">Multi-language support</p>
      </div>

      <div class="card">
        <h3 class="text-xl font-semibold mb-2">💾 Smart Cache</h3>
        <p class="text-gray-400">30-day video caching with LRU eviction</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  middleware: ['auth'],
})

const authStore = useAuthStore()
</script>

<style scoped>
.btn-lg {
  padding: 1rem 2rem;
  font-size: 1.125rem;
}
</style>

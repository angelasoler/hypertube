<template>
  <div class="container mx-auto px-4 py-8">
    <h1 class="text-4xl font-bold mb-8">Welcome to HyperTube</h1>

    <div class="card mb-8">
      <h2 class="text-2xl font-semibold mb-4">Getting Started</h2>
      <p class="text-gray-300 mb-4">
        You're now logged in! The next iteration will add video streaming functionality.
      </p>
      <p class="text-gray-400 text-sm">
        Logged in as: <span class="text-primary-400">{{ authStore.user?.username }}</span>
      </p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="card">
        <h3 class="text-xl font-semibold mb-2">🎬 Stream Videos</h3>
        <p class="text-gray-400">Coming in Iteration 2</p>
      </div>

      <div class="card">
        <h3 class="text-xl font-semibold mb-2">📝 Subtitles</h3>
        <p class="text-gray-400">Multi-language support</p>
      </div>

      <div class="card">
        <h3 class="text-xl font-semibold mb-2">💾 Cache</h3>
        <p class="text-gray-400">Smart video caching</p>
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

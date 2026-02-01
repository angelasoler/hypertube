<template>
  <div class="min-h-screen bg-gray-900">
    <nav class="bg-gray-800 border-b border-gray-700">
      <div class="container mx-auto px-4">
        <div class="flex items-center justify-between h-16">
          <div class="flex items-center space-x-8">
            <NuxtLink to="/" class="text-2xl font-bold text-primary-400">
              HyperTube
            </NuxtLink>

            <div v-if="authStore.isLoggedIn" class="hidden md:flex items-center space-x-4">
              <NuxtLink
                to="/"
                class="text-gray-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium"
              >
                Home
              </NuxtLink>
              <NuxtLink
                to="/browse"
                class="text-gray-300 hover:text-white px-3 py-2 rounded-md text-sm font-medium"
              >
                Browse
              </NuxtLink>
            </div>
          </div>

          <div v-if="authStore.isLoggedIn" class="flex items-center space-x-4">
            <span class="text-gray-300 text-sm">
              {{ authStore.user?.username }}
            </span>
            <button
              @click="handleLogout"
              class="btn btn-secondary text-sm"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </nav>

    <main>
      <slot />
    </main>

    <footer class="bg-gray-800 border-t border-gray-700 mt-auto">
      <div class="container mx-auto px-4 py-6">
        <p class="text-center text-gray-400 text-sm">
          HyperTube - Stream videos from torrents instantly
        </p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
}
</script>

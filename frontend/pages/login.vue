<template>
  <div class="min-h-screen flex items-center justify-center px-4">
    <div class="card max-w-md w-full">
      <h1 class="text-3xl font-bold text-center mb-8">Login to HyperTube</h1>

      <form @submit.prevent="handleLogin" class="space-y-6">
        <div>
          <label for="usernameOrEmail" class="block text-sm font-medium mb-2">
            Username or Email
          </label>
          <input
            id="usernameOrEmail"
            v-model="form.usernameOrEmail"
            type="text"
            required
            class="input"
            placeholder="Enter your username or email"
          />
        </div>

        <div>
          <label for="password" class="block text-sm font-medium mb-2">
            Password
          </label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            required
            class="input"
            placeholder="Enter your password"
          />
        </div>

        <div v-if="error" class="p-4 bg-red-900/50 border border-red-500 rounded-lg">
          <p class="text-sm text-red-200">{{ error }}</p>
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="btn btn-primary w-full"
        >
          <span v-if="loading">Logging in...</span>
          <span v-else>Login</span>
        </button>
      </form>

      <div class="mt-6 text-center">
        <p class="text-gray-400">
          Don't have an account?
          <NuxtLink to="/register" class="text-primary-400 hover:text-primary-300">
            Register here
          </NuxtLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { LoginRequest } from '~/types/auth'

definePageMeta({
  layout: false,
})

const authStore = useAuthStore()
const router = useRouter()

const form = ref<LoginRequest>({
  usernameOrEmail: '',
  password: '',
})

const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  error.value = ''
  loading.value = true

  try {
    await authStore.login(form.value)
    router.push('/')
  } catch (err: any) {
    error.value = err.message || 'Login failed. Please check your credentials.'
  } finally {
    loading.value = false
  }
}
</script>

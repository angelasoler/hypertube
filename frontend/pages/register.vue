<template>
  <div class="min-h-screen flex items-center justify-center px-4 py-12">
    <div class="card max-w-md w-full">
      <h1 class="text-3xl font-bold text-center mb-8">Create Account</h1>

      <form @submit.prevent="handleRegister" class="space-y-6">
        <div>
          <label for="username" class="block text-sm font-medium mb-2">
            Username *
          </label>
          <input
            id="username"
            v-model="form.username"
            type="text"
            required
            class="input"
            placeholder="Choose a username"
          />
        </div>

        <div>
          <label for="email" class="block text-sm font-medium mb-2">
            Email *
          </label>
          <input
            id="email"
            v-model="form.email"
            type="email"
            required
            class="input"
            placeholder="your@email.com"
          />
        </div>

        <div>
          <label for="password" class="block text-sm font-medium mb-2">
            Password *
          </label>
          <input
            id="password"
            v-model="form.password"
            type="password"
            required
            minlength="8"
            class="input"
            placeholder="At least 8 characters"
          />
        </div>

        <div>
          <label for="firstName" class="block text-sm font-medium mb-2">
            First Name
          </label>
          <input
            id="firstName"
            v-model="form.firstName"
            type="text"
            class="input"
            placeholder="Optional"
          />
        </div>

        <div>
          <label for="lastName" class="block text-sm font-medium mb-2">
            Last Name
          </label>
          <input
            id="lastName"
            v-model="form.lastName"
            type="text"
            class="input"
            placeholder="Optional"
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
          <span v-if="loading">Creating account...</span>
          <span v-else>Create Account</span>
        </button>
      </form>

      <div class="mt-6 text-center">
        <p class="text-gray-400">
          Already have an account?
          <NuxtLink to="/login" class="text-primary-400 hover:text-primary-300">
            Login here
          </NuxtLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RegisterRequest } from '~/types/auth'

definePageMeta({
  layout: false,
})

const authStore = useAuthStore()
const router = useRouter()

const form = ref<RegisterRequest>({
  username: '',
  email: '',
  password: '',
  firstName: '',
  lastName: '',
})

const loading = ref(false)
const error = ref('')

const handleRegister = async () => {
  error.value = ''
  loading.value = true

  try {
    await authStore.register(form.value)
    router.push('/')
  } catch (err: any) {
    error.value = err.message || 'Registration failed. Please try again.'
  } finally {
    loading.value = false
  }
}
</script>

export default defineNuxtPlugin(async () => {
  const authStore = useAuthStore()

  // Initialize auth state from localStorage
  await authStore.init()
})

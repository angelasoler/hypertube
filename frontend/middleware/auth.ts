export default defineNuxtRouteMiddleware((to, from) => {
  const authStore = useAuthStore()

  // List of public routes that don't require authentication
  const publicRoutes = ['/login', '/register']

  if (!authStore.isLoggedIn && !publicRoutes.includes(to.path)) {
    return navigateTo('/login')
  }

  // Redirect to home if already logged in and trying to access login/register
  if (authStore.isLoggedIn && publicRoutes.includes(to.path)) {
    return navigateTo('/')
  }
})

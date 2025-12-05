import { defineStore } from 'pinia'
import type { User, LoginRequest, RegisterRequest } from '~/types/auth'

interface AuthState {
  user: User | null
  token: string | null
  refreshToken: string | null
  isAuthenticated: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    token: null,
    refreshToken: null,
    isAuthenticated: false,
  }),

  getters: {
    currentUser: (state) => state.user,
    isLoggedIn: (state) => state.isAuthenticated && !!state.token,
  },

  actions: {
    async login(credentials: LoginRequest) {
      const api = useApi()

      try {
        const response = await api.auth.login(credentials)

        this.user = response.user
        this.token = response.accessToken
        this.refreshToken = response.refreshToken
        this.isAuthenticated = true

        // Store tokens in localStorage for persistence
        if (process.client) {
          localStorage.setItem('accessToken', response.accessToken)
          localStorage.setItem('refreshToken', response.refreshToken)
          localStorage.setItem('user', JSON.stringify(response.user))
        }

        return response
      } catch (error) {
        this.logout()
        throw error
      }
    },

    async register(data: RegisterRequest) {
      const api = useApi()

      try {
        const user = await api.auth.register(data)

        // Auto-login after successful registration
        await this.login({
          usernameOrEmail: data.username,
          password: data.password,
        })

        return user
      } catch (error) {
        throw error
      }
    },

    async fetchUser() {
      if (!this.token) {
        return null
      }

      const api = useApi()

      try {
        const user = await api.auth.me()
        this.user = user
        return user
      } catch (error) {
        this.logout()
        throw error
      }
    },

    async logout() {
      const api = useApi()

      try {
        if (this.token) {
          await api.auth.logout()
        }
      } catch (error) {
        console.error('Logout request failed:', error)
      } finally {
        this.user = null
        this.token = null
        this.refreshToken = null
        this.isAuthenticated = false

        // Clear localStorage
        if (process.client) {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          localStorage.removeItem('user')
        }

        // Redirect to login
        if (process.client) {
          navigateTo('/login')
        }
      }
    },

    async init() {
      // Restore auth state from localStorage on app init
      if (process.client) {
        const token = localStorage.getItem('accessToken')
        const refreshToken = localStorage.getItem('refreshToken')
        const userStr = localStorage.getItem('user')

        if (token && userStr) {
          this.token = token
          this.refreshToken = refreshToken
          this.user = JSON.parse(userStr)
          this.isAuthenticated = true

          // Verify token is still valid
          try {
            await this.fetchUser()
          } catch (error) {
            console.error('Token validation failed:', error)
            this.logout()
          }
        }
      }
    },
  },
})

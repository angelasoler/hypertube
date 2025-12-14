import { defineEventHandler, proxyRequest } from 'h3'

export default defineEventHandler(async (event) => {
  const path = event.path

  // Only proxy requests starting with /api
  if (path.startsWith('/api')) {
    const target = process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080'

    return proxyRequest(event, `${target}${path}`, {
      fetchOptions: {
        headers: {
          ...event.headers,
        },
      },
    })
  }
})

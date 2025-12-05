# HyperTube Frontend

Vue.js/Nuxt 3 frontend for the HyperTube video streaming platform.

## Setup

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at http://localhost:3000

## Environment Variables

Create a `.env` file:

```
NUXT_PUBLIC_API_BASE=http://localhost:8080
```

## Features

### Iteration 1: Authentication ✅
- User registration and login
- JWT token management
- Protected routes with middleware
- Persistent auth state

### Iteration 2: Video Player (Coming Next)
- Video streaming from torrents
- Progress monitoring
- Subtitle support
- Download status display

## Project Structure

```
frontend/
├── assets/          # Static assets (CSS, images)
├── components/      # Vue components
├── composables/     # Composable functions (useApi, etc.)
├── layouts/         # Page layouts
├── middleware/      # Route middleware
├── pages/           # Application pages (auto-routed)
├── plugins/         # Nuxt plugins
├── stores/          # Pinia stores
├── types/           # TypeScript type definitions
└── nuxt.config.ts   # Nuxt configuration
```

## Technologies

- **Nuxt 3** - Vue.js framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **Pinia** - State management
- **Fetch API** - HTTP client

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-01-01',

  // Single-page app: a dashboard that talks to the LiftLens REST API and deploys static to
  // Cloudflare Pages. SSR off keeps ECharts client-only and the build a simple static bundle.
  ssr: false,

  modules: ['@pinia/nuxt'],

  css: ['~/assets/css/main.css'],

  // Auto-import the Pinia stores so components can use them without explicit imports.
  imports: {
    dirs: ['stores'],
  },

  runtimeConfig: {
    public: {
      // Override at deploy time via NUXT_PUBLIC_API_BASE / NUXT_PUBLIC_API_TOKEN.
      apiBase: 'http://localhost:8080',
      apiToken: 'dev-api-token',
    },
  },

  app: {
    head: {
      title: 'LiftLens',
      htmlAttrs: { lang: 'en' },
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: 'An analytics lens over your lifting history.' },
      ],
    },
  },

  devtools: { enabled: false },
})

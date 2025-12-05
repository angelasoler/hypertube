<script setup lang="ts">
// Redirect to the new browse page
definePageMeta({
  middleware: ['auth'],
})

// Immediate redirect
navigateTo('/browse', { replace: true })
</script>

<template>
  <div></div>
</template>

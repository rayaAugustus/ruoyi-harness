<script setup>
import AiBuilderView from '@harness-ui/ai/AiBuilderView.vue'
import useUserStore from '@/store/modules/user'
defineOptions({ name: 'HarnessAiBuilder' })
const userStore = useUserStore()
const canPublish = userStore.permissions.includes('*:*:*') || userStore.permissions.includes('harness:app:publish')
</script>
<template><div class="app-container"><AiBuilderView :can-publish="canPublish" /></div></template>

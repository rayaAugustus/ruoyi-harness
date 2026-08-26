<script setup>
import { useRoute, useRouter } from 'vue-router'
import HarnessApplicationDetailView from '@harness-ui/admin/ApplicationDetailView.vue'

defineOptions({ name: 'HarnessAppDetail' })
const route = useRoute(), router = useRouter()
const appKey = String(route.params.appKey)
const edit = id => router.push(`/harness/script/${encodeURIComponent(appKey)}/${id}`)
</script>

<template><div class="app-container"><HarnessApplicationDetailView :app-key="appKey" @edit="edit" /></div></template>

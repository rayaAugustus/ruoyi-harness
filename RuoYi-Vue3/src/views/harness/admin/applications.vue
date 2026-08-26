<script setup>
import { useRouter } from 'vue-router'
import HarnessApplicationsView from '@harness-ui/admin/ApplicationsView.vue'

defineOptions({ name: 'HarnessApplications' })
const router = useRouter()
const open = appKey => router.push(`/harness/app-detail/${encodeURIComponent(appKey)}`)
</script>

<template><div class="app-container"><HarnessApplicationsView @open="open" /></div></template>

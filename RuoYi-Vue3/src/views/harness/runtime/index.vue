<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import HarnessDynamicAppView from '@harness-ui/runtime/HarnessDynamicAppView.vue'

defineOptions({ name: 'HarnessRuntime' })
const route = useRoute()
const appKey = computed(() => String(route.params.appKey || route.path.split('/').filter(Boolean).at(-1) || ''))
</script>

<template>
  <div class="app-container"><HarnessDynamicAppView :app-key="appKey" :route-state="route.query" /></div>
</template>

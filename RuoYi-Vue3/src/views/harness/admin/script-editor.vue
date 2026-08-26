<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import HarnessScriptEditorView from '@harness-ui/admin/ScriptEditorView.vue'

defineOptions({ name: 'HarnessScriptEditor' })
const route = useRoute()
const appKey = computed(() => String(route.params.appKey))
const versionId = computed(() => Number(route.params.versionId))
const compareVersionId = computed(() => route.query.compare ? Number(route.query.compare) : undefined)
</script>

<template><div class="app-container"><HarnessScriptEditorView :app-key="appKey" :version-id="versionId" :compare-version-id="compareVersionId" /></div></template>

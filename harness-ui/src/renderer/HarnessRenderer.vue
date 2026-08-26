<script setup lang="ts">import type {PageNode,ActionBinding} from '../types';import HarnessNode from './HarnessNode.vue';defineProps<{page:PageNode;state:Record<string,any>}>();defineEmits<{action:[ActionBinding]}>();</script>
<template><HarnessNode :node="page" :state="state" @action="$emit('action',$event)"/></template>

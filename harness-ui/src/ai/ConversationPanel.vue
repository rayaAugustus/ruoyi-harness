<script setup lang="ts">
import {ref} from 'vue';import type {AiStoredMessage} from '../types';
defineProps<{messages:AiStoredMessage[];generating:boolean}>();const emit=defineEmits<{send:[string]}>();const input=ref('');
const send=()=>{const value=input.value.trim();if(!value)return;emit('send',value);input.value=''};
</script>
<template><section class="ai-conversation"><header><h3>Conversation</h3></header><div class="ai-message-list"><p v-if="!messages.length" class="muted">Describe the business page you want to build.</p><article v-for="message in messages" :key="message.id" :class="['ai-message',message.role.toLowerCase()]" :data-role="message.role"><strong>{{message.role==='USER'?'You':message.role==='ASSISTANT'?'AI':'System'}}</strong><p>{{message.content}}</p></article></div><div class="ai-composer"><textarea v-model="input" :disabled="generating" rows="4" placeholder="Create a customer dashboard…" @keydown.ctrl.enter.prevent="send"/><button :disabled="generating||!input.trim()" @click="send">{{generating?'Generating…':'Send'}}</button></div></section></template>

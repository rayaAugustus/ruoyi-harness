<script setup lang="ts">
import {computed} from 'vue';import type {HarnessNode,ActionBinding} from '../types';import {componentRegistry} from './components';
const props=defineProps<{node:HarnessNode;state:Record<string,any>;formId?:string}>();const emit=defineEmits<{action:[ActionBinding]}>();
const component=computed(()=>componentRegistry[props.node.type as keyof typeof componentRegistry]);
const children=computed(()=>props.node.type==='form'?[...(props.node.fields||[]),...(props.node.children||[])]:props.node.children||props.node.fields||[]);const currentForm=computed(()=>props.node.type==='form'?props.node.id:props.formId);
const model=computed({get:()=>currentForm.value?props.state[currentForm.value]?.[props.node.name]??props.node.value:props.node.value,set:value=>{if(currentForm.value){props.state[currentForm.value]??={};props.state[currentForm.value][props.node.name]=value}}});
</script>
<template>
  <div v-if="!component" class="harness-render-error" role="alert">Unsupported component: {{ node.type }}</div>
  <component v-else :is="component" v-bind="node" v-model="model" @activate="node.action&&emit('action',node.action)" @submit.prevent>
    <h1 v-if="node.type==='page'">{{ node.title }}</h1><h2 v-else-if="node.type==='section'">{{ node.title }}</h2>
    <template v-if="node.type==='tabs'"><section v-for="(tab,i) in node.items" :key="i"><h3>{{tab.label}}</h3><HarnessNode v-for="(child,j) in tab.children" :key="j" :node="child" :state="state" :form-id="currentForm" @action="emit('action',$event)"/></section></template>
    <HarnessNode v-for="(child,i) in children" :key="child.id||i" :node="child" :state="state" :form-id="currentForm" @action="emit('action',$event)"/>
  </component>
</template>

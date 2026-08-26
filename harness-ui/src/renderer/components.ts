import {defineComponent,h} from 'vue';
const shell=(name:string,tag='div')=>defineComponent({name,inheritAttrs:false,setup(_,ctx){return()=>h(tag,{class:`harness-${name.toLowerCase()}`},ctx.slots.default?.())}});
export const HarnessPage=shell('Page','main');export const HarnessSection=shell('Section','section');export const HarnessForm=shell('Form','form');
export const HarnessText=defineComponent({name:'HarnessText',props:{value:{required:true},variant:String},setup:p=>()=>h('span',{class:`harness-text ${p.variant||''}`},String(p.value))});
export const HarnessStatistic=defineComponent({name:'HarnessStatistic',props:{label:String,value:{required:true},suffix:String},setup:p=>()=>h('div',{class:'harness-statistic'},[h('small',p.label),h('strong',String(p.value)+(p.suffix||''))])});
export const HarnessTable=defineComponent({
  name:'HarnessTable',
  props:{columns:{type:Array,required:true},rows:{type:Array,required:true},emptyText:String},
  setup:p=>()=>h('table',{class:'harness-table'},[
    h('thead',h('tr',(p.columns as any[]).map(c=>h('th',String(c.label))))),
    h('tbody',(p.rows as any[]).length
      ?(p.rows as any[]).map(r=>h('tr',(p.columns as any[]).map(c=>h('td',String(r[c.key]??'')))))
      :[h('tr',h('td',{colspan:(p.columns as any[]).length},p.emptyText||'No data'))])
  ])
});
export const HarnessInput=defineComponent({name:'HarnessInput',props:{modelValue:{},label:String,name:String,inputType:String,required:Boolean,placeholder:String},emits:['update:modelValue'],setup:(p,{emit})=>()=>h('label',[h('span',p.label),h('input',{value:p.modelValue,type:p.inputType||'text',required:p.required,placeholder:p.placeholder,onInput:(e:any)=>emit('update:modelValue',e.target.value)})])});
export const HarnessSelect=defineComponent({name:'HarnessSelect',props:{modelValue:{},label:String,name:String,options:Array,required:Boolean},emits:['update:modelValue'],setup:(p,{emit})=>()=>h('label',[h('span',p.label),h('select',{value:p.modelValue,required:p.required,onChange:(e:any)=>emit('update:modelValue',e.target.value)},(p.options as any[]||[]).map(o=>h('option',{value:o.value},o.label)))])});
export const HarnessButton=defineComponent({name:'HarnessButton',props:{text:String,variant:String,disabled:Boolean},emits:['activate'],setup:(p,{emit})=>()=>h('button',{type:'button',disabled:p.disabled,class:`harness-button ${p.variant||'default'}`,onClick:()=>emit('activate')},p.text)});
export const HarnessTabs=shell('Tabs');export const HarnessModal=shell('Modal');
export const HarnessAlert=defineComponent({name:'HarnessAlert',props:{title:String,message:String,variant:String},setup:p=>()=>h('div',{role:'alert',class:`harness-alert ${p.variant||'info'}`},[p.title&&h('strong',p.title),h('span',p.message)])});
export const HarnessChart=defineComponent({name:'HarnessChart',props:{title:String,chartType:String,labels:Array,series:Array},setup:p=>()=>h('figure',{class:'harness-chart'},[p.title&&h('figcaption',p.title),h('pre',JSON.stringify({type:p.chartType,labels:p.labels,series:p.series},null,2))])});
export const componentRegistry={page:HarnessPage,section:HarnessSection,text:HarnessText,statistic:HarnessStatistic,table:HarnessTable,form:HarnessForm,input:HarnessInput,select:HarnessSelect,button:HarnessButton,tabs:HarnessTabs,modal:HarnessModal,alert:HarnessAlert,chart:HarnessChart} as const;

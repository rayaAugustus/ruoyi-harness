import {mount,flushPromises} from '@vue/test-utils';
import {describe,it,expect,beforeEach} from 'vitest';
import HarnessRenderer from '../src/renderer/HarnessRenderer.vue';
import HarnessDynamicAppView from '../src/runtime/HarnessDynamicAppView.vue';
import {configureHarnessTransport} from '../src/runtime-client/api';

describe('Harness renderer security and actions',()=>{
  it('renders normal text as text rather than HTML',()=>{const wrapper=mount(HarnessRenderer,{props:{page:{type:'page',title:'Safe',children:[{type:'text',value:'<script>window.evil=1</script>'}]},state:{}}});expect(wrapper.find('script').exists()).toBe(false);expect(wrapper.text()).toContain('<script>window.evil=1</script>')});
  it('shows controlled error for unknown component',()=>{const wrapper=mount(HarnessRenderer,{props:{page:{type:'page',title:'Safe',children:[{type:'iframe'}]},state:{}}});expect(wrapper.get('[role=alert]').text()).toContain('Unsupported component')});
  it('passes pinned version and resolved form state to actions',async()=>{const calls:{url:string;body:any}[]=[];configureHarnessTransport(async(url,init)=>{calls.push({url,body:init?.body?JSON.parse(String(init.body)):null});if(url.endsWith('/render'))return {appKey:'form-app',versionId:42,traceId:'t',page:{type:'page',title:'Form',children:[{type:'form',id:'customer',fields:[{type:'input',name:'name',label:'Name'}],children:[{type:'button',text:'Save',action:{name:'save',input:{source:'form.customer'}}}]}]}};return {appKey:'form-app',versionId:42,traceId:'a',effects:{toast:{message:'Saved'}}}});const wrapper=mount(HarnessDynamicAppView,{props:{appKey:'form-app'}});await flushPromises();await wrapper.get('input').setValue('Acme');await wrapper.get('button').trigger('click');await flushPromises();const action=calls.find(c=>c.url.includes('/actions/save'))!;expect(action.body.versionId).toBe(42);expect(action.body.input).toEqual({name:'Acme'});expect(wrapper.text()).toContain('Saved')});
});

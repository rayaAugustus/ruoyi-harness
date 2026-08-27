import {mount,flushPromises} from '@vue/test-utils';
import {describe,it,expect} from 'vitest';
import AiBuilderView from '../src/ai/AiBuilderView.vue';
import {configureHarnessTransport} from '../src/runtime-client/api';

const session={session:{sessionKey:'s1',title:'Customer dashboard',status:'ACTIVE',createdAt:'now',updatedAt:'now'},messages:[]};

describe('Harness AI Builder',()=>{
  it('creates a session, generates, validates and previews through the shared renderer',async()=>{const calls:string[]=[];configureHarnessTransport(async(url,init)=>{calls.push(url);
    if(url.endsWith('/status'))return {enabled:true,provider:'openai-compatible',model:'m',endpointConfigured:true,apiKeyConfigured:true};
    if(url==='/harness/apps'||url==='/harness/capabilities')return [];
    if(url==='/harness/ai/sessions'&&init?.method==='POST')return structuredClone(session);
    if(url==='/harness/ai/sessions')return [];
    if(url.endsWith('/messages'))return {sessionKey:'s1',assistantMessage:'Created the dashboard',appKey:'customer-dashboard',versionId:7,versionNo:1,source:"defineApp({page:()=>page({title:'Customers',children:[]})});",validation:{valid:true,diagnostics:[],sourceHash:'sha256:x'},capabilitiesUsed:[]};
    if(url.endsWith('/preview'))return {appKey:'customer-dashboard',versionId:7,traceId:'t',page:{type:'page',title:'Customers',children:[{type:'text',value:'Acme'}]}};
    if(url.endsWith('/publish'))return {appKey:'customer-dashboard',versionId:7,versionNo:1};throw new Error(url)});
    const wrapper=mount(AiBuilderView,{props:{canPublish:true}});await flushPromises();await wrapper.get('.ai-start input').setValue('Customer dashboard');await wrapper.get('.ai-start button').trigger('click');await flushPromises();
    await wrapper.get('.ai-composer textarea').setValue('Create a customer dashboard');await wrapper.get('.ai-composer button').trigger('click');await flushPromises();
    expect(wrapper.text()).toContain('Created the dashboard');expect(wrapper.text()).toContain('Acme');expect(wrapper.text()).toContain('Passed');expect((wrapper.get('.ai-source textarea').element as HTMLTextAreaElement).value).toContain('defineApp');expect(wrapper.text()).toContain('Unpublished changes');expect(calls.some(url=>url.endsWith('/preview'))).toBe(true);
    const publish=wrapper.findAll('button').find(button=>button.text()==='Publish')!;expect(publish.attributes('disabled')).toBeUndefined();await publish.trigger('click');await flushPromises();expect(wrapper.text()).toContain('Published successfully');});

  it('shows stable provider error codes instead of hiding generation failure',async()=>{configureHarnessTransport(async(url,init)=>{if(url.endsWith('/status'))return {enabled:true};if(url==='/harness/apps'||url==='/harness/capabilities')return [];if(url==='/harness/ai/sessions'&&init?.method==='POST')return structuredClone(session);if(url==='/harness/ai/sessions')return [];if(url.endsWith('/messages'))throw {code:'AI_RATE_LIMITED',message:'Try later'};return {}});
    const wrapper=mount(AiBuilderView);await flushPromises();await wrapper.get('.ai-start button').trigger('click');await flushPromises();await wrapper.get('.ai-composer textarea').setValue('Build it');await wrapper.get('.ai-composer button').trigger('click');await flushPromises();expect(wrapper.get('.ai-global-error').text()).toContain('AI_RATE_LIMITED');});

  it('lists persisted sessions and restores the conversation and draft after refresh',async()=>{configureHarnessTransport(async(url)=>{if(url.endsWith('/status'))return {enabled:true};if(url==='/harness/apps'||url==='/harness/capabilities')return [];if(url==='/harness/ai/sessions')return [{sessionKey:'old-session',title:'Saved customer app',status:'ACTIVE',appKey:'saved-app',activeVersionId:12,createdAt:'2026-08-20T10:00:00Z',updatedAt:'2026-08-21T10:00:00Z'}];if(url.endsWith('/old-session'))return {session:{sessionKey:'old-session',title:'Saved customer app',status:'ACTIVE',appKey:'saved-app',activeVersionId:12,createdAt:'2026-08-20T10:00:00Z',updatedAt:'2026-08-21T10:00:00Z'},activeVersion:{id:12,appId:4,versionNo:3,sdkVersion:'1',source:"defineApp({page:()=>page({title:'Restored',children:[]})});",status:'VALIDATED'},messages:[{id:5,role:'ASSISTANT',content:'The saved conversation is back.',createdAt:'2026-08-21T10:00:00Z'}]};throw new Error(url)});
    const wrapper=mount(AiBuilderView);await flushPromises();expect(wrapper.text()).toContain('Saved customer app');const resume=wrapper.findAll('button').find(button=>button.text()==='Continue')!;await resume.trigger('click');await flushPromises();expect(wrapper.text()).toContain('The saved conversation is back.');expect((wrapper.get('.ai-source textarea').element as HTMLTextAreaElement).value).toContain("title:'Restored'");expect(wrapper.text()).toContain('Version 3 · VALIDATED');expect(wrapper.text()).toContain('← Sessions');});
});

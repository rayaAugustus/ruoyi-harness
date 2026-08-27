import type {ActionBinding,AiGenerationResponse,AiSessionView,AiStatus,AppDescriptor,Json,RuntimeResponse,ValidationResult,VersionDescriptor} from '../types';
export type Transport=(url:string,init?:RequestInit)=>Promise<any>;
let transport:Transport=async(url,init={})=>{const response=await fetch(url,{credentials:'same-origin',...init,headers:{'Content-Type':'application/json',...(init.headers||{})}});const body=await response.json();if(!response.ok)throw body;return body?.data??body;};
export const configureHarnessTransport=(value:Transport)=>{transport=value};
const json=(method:string,body?:unknown):RequestInit=>({method,body:body===undefined?undefined:JSON.stringify(body)});
export const harnessApi={
  listApps:()=>transport('/harness/apps') as Promise<AppDescriptor[]>,createApp:(body:Partial<AppDescriptor>)=>transport('/harness/apps',json('POST',body)),
  getApp:(key:string)=>transport(`/harness/apps/${key}`) as Promise<AppDescriptor>,updateApp:(key:string,body:Partial<AppDescriptor>)=>transport(`/harness/apps/${key}`,json('PUT',body)),
  setEnabled:(key:string,enabled:boolean)=>transport(`/harness/apps/${key}/${enabled?'enable':'disable'}`,json('POST')),
  listVersions:(key:string)=>transport(`/harness/apps/${key}/versions`) as Promise<VersionDescriptor[]>,createVersion:(key:string,source:string)=>transport(`/harness/apps/${key}/versions`,json('POST',{sdkVersion:'1',source})),
  getVersion:(key:string,id:number)=>transport(`/harness/apps/${key}/versions/${id}`) as Promise<VersionDescriptor>,saveSource:(key:string,id:number,source:string)=>transport(`/harness/apps/${key}/versions/${id}/source`,json('PUT',{source})),
  validate:(key:string,id:number)=>transport(`/harness/apps/${key}/versions/${id}/validate`,json('POST')),publish:(key:string,id:number)=>transport(`/harness/apps/${key}/versions/${id}/publish`,json('POST')),
  rollback:(key:string,id:number)=>transport(`/harness/apps/${key}/rollback/${id}`,json('POST')),deleteVersion:(key:string,id:number)=>transport(`/harness/apps/${key}/versions/${id}`,json('DELETE')),
  render:(key:string,route:Json={},state:Json={},versionId?:number)=>transport(`/harness/runtime/apps/${key}/render`,json('POST',{route,state,versionId})) as Promise<RuntimeResponse>,
  action:(key:string,action:ActionBinding,versionId:number,input:Json,state:Json)=>transport(`/harness/runtime/apps/${key}/actions/${action.name}`,json('POST',{versionId,input,clientState:state})) as Promise<RuntimeResponse>,
  executions:(query='')=>transport(`/harness/audit/executions${query}`),capabilityLogs:(query='')=>transport(`/harness/audit/capabilities${query}`),capabilities:()=>transport('/harness/capabilities'),
  aiStatus:()=>transport('/harness/ai/status') as Promise<AiStatus>,aiSessions:()=>transport('/harness/ai/sessions'),
  createAiSession:(body:{appKey?:string|null;title:string})=>transport('/harness/ai/sessions',json('POST',body)) as Promise<AiSessionView>,
  getAiSession:(key:string)=>transport(`/harness/ai/sessions/${key}`) as Promise<AiSessionView>,
  sendAiMessage:(key:string,message:string)=>transport(`/harness/ai/sessions/${key}/messages`,json('POST',{message})) as Promise<AiGenerationResponse>,
  previewAi:(key:string,body:Record<string,unknown>={})=>transport(`/harness/ai/sessions/${key}/preview`,json('POST',body)) as Promise<RuntimeResponse>,
  validateAi:(key:string)=>transport(`/harness/ai/sessions/${key}/validate`,json('POST')) as Promise<ValidationResult>,
  publishAi:(key:string)=>transport(`/harness/ai/sessions/${key}/publish`,json('POST')),archiveAi:(key:string)=>transport(`/harness/ai/sessions/${key}/archive`,json('POST'))
};

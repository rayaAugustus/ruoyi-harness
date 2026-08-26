import type {ActionBinding,AppDescriptor,Json,RuntimeResponse,VersionDescriptor} from '../types';
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
  render:(key:string,route:Json={},state:Json={})=>transport(`/harness/runtime/apps/${key}/render`,json('POST',{route,state})) as Promise<RuntimeResponse>,
  action:(key:string,action:ActionBinding,versionId:number,input:Json,state:Json)=>transport(`/harness/runtime/apps/${key}/actions/${action.name}`,json('POST',{versionId,input,clientState:state})) as Promise<RuntimeResponse>,
  executions:(query='')=>transport(`/harness/audit/executions${query}`),capabilityLogs:(query='')=>transport(`/harness/audit/capabilities${query}`),capabilities:()=>transport('/harness/capabilities')
};

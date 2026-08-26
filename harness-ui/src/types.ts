export type Json = null|string|number|boolean|Json[]|{[key:string]:Json};
export interface ActionBinding {name:string;input?:Json}
export interface BaseNode {type:string;id?:string}
export interface PageNode extends BaseNode {type:'page';title:string;children:HarnessNode[]}
export type HarnessNode = BaseNode & Record<string,any>;
export interface RuntimeResponse {appKey:string;versionId:number;traceId:string;page?:PageNode;effects?:Record<string,any>}
export interface AppDescriptor {id:number;appKey:string;name:string;description?:string;routeTitle:string;icon?:string;orderNum?:number;requiredPermission:string;enabled:boolean;publishedVersionId?:number}
export interface VersionDescriptor {id:number;appId:number;versionNo:number;sdkVersion:string;source:string;sourceHash?:string;status:'DRAFT'|'VALIDATED'|'REJECTED'|'PUBLISHED'|'SUPERSEDED';diagnostics?:Diagnostic[]}
export interface Diagnostic {severity:string;code:string;message:string;line?:number;column?:number}

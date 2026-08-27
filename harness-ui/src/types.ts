export type Json = null|string|number|boolean|Json[]|{[key:string]:Json};
export interface ActionBinding {name:string;input?:Json}
export interface BaseNode {type:string;id?:string}
export interface PageNode extends BaseNode {type:'page';title:string;children:HarnessNode[]}
export type HarnessNode = BaseNode & Record<string,any>;
export interface RuntimeResponse {appKey:string;versionId:number;traceId:string;page?:PageNode;effects?:Record<string,any>}
export interface AppDescriptor {id:number;appKey:string;name:string;description?:string;routeTitle:string;icon?:string;orderNum?:number;requiredPermission:string;enabled:boolean;publishedVersionId?:number}
export interface VersionDescriptor {id:number;appId:number;versionNo:number;sdkVersion:string;source:string;sourceHash?:string;status:'DRAFT'|'VALIDATED'|'REJECTED'|'PUBLISHED'|'SUPERSEDED';diagnostics?:Diagnostic[]}
export interface Diagnostic {severity:string;code:string;message:string;line?:number;column?:number}
export interface ValidationResult {valid:boolean;diagnostics:Diagnostic[];sourceHash?:string}
export interface AiStatus {enabled:boolean;provider:string;model:string;endpointConfigured:boolean;apiKeyConfigured:boolean}
export interface AiStoredMessage {id:number;role:'USER'|'ASSISTANT'|'SYSTEM_EVENT';content:string;scriptSnapshot?:string;model?:string;provider?:string;inputTokens?:number;outputTokens?:number;createdAt:string}
export interface AiSessionSummary {sessionKey:string;title:string;status:'ACTIVE'|'ARCHIVED';appKey?:string;activeVersionId?:number;createdAt:string;updatedAt:string}
export interface AiSessionView {session:AiSessionSummary;app?:AppDescriptor;activeVersion?:VersionDescriptor;messages:AiStoredMessage[]}
export interface AiGenerationResponse {sessionKey:string;assistantMessage:string;appKey:string;versionId:number;versionNo:number;source:string;validation:ValidationResult;capabilitiesUsed:string[]}
export interface CapabilityView {name:string;version:string;description?:string;requiredPermission?:string;riskLevel:'READ'|'WRITE'|'SENSITIVE_WRITE'|'ADMIN'}

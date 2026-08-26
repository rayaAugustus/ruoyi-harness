# RuoYi Harness — Phase 2: AI Builder Development Specification

## 1. Objective

Phase 2 turns the existing Harness runtime into a usable product by adding an **AI Builder** that converts natural-language requirements into Harness JavaScript applications.

The user experience must become:

```text
User requirement
    |
    v
AI Builder
    |
    +-- reads Harness SDK contract
    +-- reads available capabilities
    +-- reads current app/draft when editing
    |
    v
Generate / modify Harness script
    |
    v
Server-side validate
    |
    v
Preview
    |
    +-- user asks for changes -> regenerate/patch -> validate -> preview
    |
    v
Explicit Publish
    |
    v
Existing Harness Runtime
```

The core product promise is:

> A user describes the business page or workflow they want; AI writes the Harness script; the existing runtime validates and runs it without rebuilding the frontend or restarting RuoYi.

Phase 2 is **not** a redesign of Phase 1. The script runtime, Capability Bridge, immutable version model, RuoYi RBAC, renderer and audit model remain authoritative.

---

## 2. Non-goals

Do not expand Phase 2 into:

- an autonomous enterprise agent platform
- a new workflow/BPM engine
- a general-purpose low-code platform
- arbitrary code generation outside the Harness SDK
- Java source generation
- Docker/Kubernetes deployment
- microservices
- dynamic JAR loading
- direct SQL generation/execution
- direct browser DOM generation
- automatic production publication without explicit user intent
- an LLM dependency inside normal Harness runtime execution

AI Builder is an **authoring layer**. Harness Runtime remains the **execution layer**.

---

## 3. Phase 1 prerequisites

Before Phase 2 is considered complete, preserve/fix these Phase 1 invariants:

1. Rollback targets must be previously published immutable versions only. `VALIDATED` must not be accepted as a rollback target.
2. A page opened on version `vN` remains pinned to that immutable version for internal refresh/action execution until explicit navigation or full reload.
3. Concurrent publish/rollback operations must serialize or use optimistic/CAS protection so two administrators cannot corrupt publication state.

Phase 2 code must not weaken these guarantees.

---

## 4. Product UX

Add a new top-level Harness administration entry:

```text
Harness
├─ AI Builder
├─ Applications
├─ Capabilities
└─ Execution Logs
```

### 4.1 New application flow

The user opens **AI Builder** and enters a requirement such as:

```text
Create a customer dashboard.
Show customer name and level in a table.
Add a refresh button.
```

The Builder should:

1. understand the request,
2. inspect available capabilities,
3. generate a valid Harness script,
4. create an app/draft if needed,
5. validate the generated script,
6. display diagnostics when validation fails,
7. render a preview,
8. allow conversational modifications,
9. publish only when the user explicitly clicks Publish.

### 4.2 Existing application flow

The user can open an existing Harness app in AI Builder and say:

```text
Add a filter for customer level and move the refresh button above the table.
```

The Builder receives the current immutable or draft source as context and returns a new draft. It must never mutate a published version in place.

### 4.3 Builder workspace

Recommended desktop layout:

```text
+----------------------+-----------------------------------------+
| Conversation         | Preview                                 |
|                      |                                         |
| User: build...       |   rendered Harness page                 |
| AI: generated...     |                                         |
| User: change...      |                                         |
|                      |                                         |
+----------------------+-----------------------------------------+
| Draft source / diagnostics / capabilities used                 |
+----------------------------------------------------------------+
| [Save Draft] [Validate] [Publish]                               |
+----------------------------------------------------------------+
```

The source panel may be collapsible, but must remain available for developers and debugging.

---

## 5. Architecture

Phase 2 adds an authoring subsystem beside the existing runtime:

```text
                    RuoYi-Harness

        +--------------------------------+
        | Existing Phase 1               |
        |                                |
        | App Registry                   |
        | Version Service                |
        | Script Runtime                 |
        | Capability Bridge              |
        | Vue Renderer                   |
        +---------------+----------------+
                        ^
                        |
                generated draft
                        |
        +---------------+----------------+
        | Phase 2 AI Builder             |
        |                                |
        | Builder Session Service        |
        | Context Assembler              |
        | Capability Catalog             |
        | Harness SDK Prompt Contract    |
        | LLM Gateway                    |
        | Generation Validator           |
        | Preview Service                |
        +---------------+----------------+
                        ^
                        |
                  Builder UI
```

The Builder must use public/internal service interfaces from Phase 1. It must not bypass `VersionService`, `HarnessScriptEngine`, `CapabilityRegistry`, publication permissions or audit rules.

---

## 6. Backend module layout

Recommended new modules/packages:

```text
RuoYi-Vue/
├─ harness-ai/
│  └─ src/main/java/com/ruoyi/harness/ai/
│     ├─ model/
│     ├─ provider/
│     ├─ prompt/
│     ├─ context/
│     ├─ service/
│     └─ port/
│
└─ harness-ruoyi-adapter/
   └─ .../web/HarnessAiBuilderController.java
```

Do not place provider-specific LLM code in `harness-core` or `harness-runtime`.

`harness-ai` may depend on:

- `harness-api`
- `harness-core`
- `harness-capability`

It must not become a dependency of `harness-runtime`.

Normal runtime execution must still work with AI completely disabled.

---

## 7. LLM provider abstraction

Define a provider-independent interface.

Conceptual API:

```java
public interface HarnessAiModel {
    AiGenerationResult generate(AiGenerationRequest request);
}
```

Request:

```java
public record AiGenerationRequest(
    List<AiMessage> messages,
    AiGenerationContext context,
    AiGenerationOptions options
) {}
```

Result:

```java
public record AiGenerationResult(
    String assistantMessage,
    String script,
    List<String> capabilitiesUsed,
    String model,
    String provider,
    Usage usage
) {}
```

### 7.1 Initial provider contract

Implement an **OpenAI-compatible HTTP provider** first so deployments can use providers exposing the common chat-completions style API.

Configuration should be externalized:

```yaml
harness:
  ai:
    enabled: true
    provider: openai-compatible
    base-url: ${HARNESS_AI_BASE_URL:}
    api-key: ${HARNESS_AI_API_KEY:}
    model: ${HARNESS_AI_MODEL:}
    temperature: 0.2
    max-output-tokens: 12000
    connect-timeout: 10s
    read-timeout: 120s
```

Never store API keys in source control or return them to the browser.

The provider abstraction must allow additional adapters later without changing Builder business logic.

---

## 8. AI context assembly

The model must not receive the entire repository on each request.

Build a compact deterministic context containing only what is needed to author Harness apps.

### 8.1 Required context

Every generation request includes:

1. **Harness SDK version**
2. **script entry contract** (`defineApp`)
3. **supported UI component contract**
4. **action/effect contract**
5. **available Capability definitions** relevant to the user/request
6. **current app metadata** when editing an app
7. **current draft/source** when modifying an existing script
8. **latest validation diagnostics** when asking AI to repair invalid code
9. **hard security constraints**

### 8.2 Capability catalog projection

Do not expose Java handler objects or implementation internals.

Provide the model a safe projection:

```json
{
  "name": "example.customer.list",
  "version": "1",
  "description": "List example customers",
  "inputSchema": {},
  "outputSchema": {},
  "requiredPermission": "example:customer:list",
  "riskLevel": "READ"
}
```

The model should prefer existing capabilities and must not invent unavailable capability names.

### 8.3 Capability selection

For small installations, sending the complete catalog is acceptable.

For large installations, introduce deterministic filtering/search by:

- capability name
- description
- schema field names
- app/domain prefix

Do not add vector infrastructure solely for Phase 2. A simple indexed/text search abstraction is sufficient.

---

## 9. Harness authoring prompt contract

The system prompt is part of the product contract and should be versioned in source control.

Recommended location:

```text
RuoYi-Vue/harness-ai/src/main/resources/prompts/builder-system-v1.md
```

The prompt must instruct the model that:

- output is Harness JavaScript, not arbitrary JavaScript,
- exactly one `defineApp({...})` registration is required,
- ES modules/import/export/require are forbidden,
- Java/JVM/browser globals are unavailable,
- direct network/database/filesystem access is forbidden,
- host functionality is available only through listed `harness.call()` capabilities,
- only supported UI components may be returned,
- no raw HTML or script injection is allowed,
- output must remain JSON-boundary compatible,
- published versions are immutable,
- it must not invent capabilities,
- it should keep scripts simple and deterministic.

The model must be told to return structured output, not Markdown code fences as the transport format.

---

## 10. Structured AI output

Do not parse arbitrary prose to extract source code.

Require a structured response equivalent to:

```json
{
  "assistantMessage": "Created a customer dashboard using the customer list capability.",
  "script": "defineApp({...});",
  "capabilitiesUsed": [
    "example.customer.list"
  ]
}
```

If the configured provider supports native structured output / JSON schema, use it.

Otherwise require strict JSON and validate it server-side.

Reject responses when:

- `script` is missing,
- `script` exceeds runtime source limits,
- the response is not valid structured output,
- a declared capability does not exist,
- model output attempts to modify host configuration rather than produce Harness script.

Never execute raw model text before normal Harness validation.

---

## 11. Generation pipeline

A generation turn must follow this pipeline:

```text
1. authenticate user
2. authorize Builder access
3. load builder session
4. load current app/draft if any
5. assemble SDK + UI + capability context
6. call LLM provider
7. parse structured response
8. static checks
9. save result as DRAFT
10. run existing VersionService / ScriptEngine validation
11. store diagnostics
12. return draft + diagnostics to UI
```

The LLM never writes directly to `harness_app_version` tables.

All app/version mutations must go through Phase 1 services.

---

## 12. Iterative editing model

A Builder conversation is attached to zero or one Harness app.

Recommended behavior:

- First successful generation can create a new Harness app and first draft.
- Subsequent turns modify the active draft.
- If the active version has already been published, the next edit creates a new draft version automatically.
- Each successful AI generation updates only the active mutable draft.
- The previous generated source must be retained in Builder turn history for debugging/audit, but immutable published app versions remain the authoritative application history.

The user can always manually edit the generated source in the existing script editor.

---

## 13. Builder session persistence

Add two tables.

### `harness_ai_session`

```text
id                  bigint PK
session_key         varchar unique
app_id              bigint nullable
active_version_id   bigint nullable
created_by           bigint
created_at           timestamp
updated_at           timestamp
title                varchar
status               varchar     # ACTIVE / ARCHIVED
```

### `harness_ai_message`

```text
id                  bigint PK
session_id          bigint
role                varchar     # USER / ASSISTANT / SYSTEM_EVENT
content             text
script_snapshot      longtext nullable
model                varchar nullable
provider             varchar nullable
input_tokens         bigint nullable
output_tokens        bigint nullable
created_at           timestamp
```

Do not persist provider API keys, hidden provider credentials, raw authorization headers or unrestricted internal prompts in message rows.

For model-debug logging, use configurable redaction and keep it disabled by default in production.

---

## 14. Preview model

Preview must use the **same renderer and script engine** as production. Do not build a second fake UI interpreter.

However, previewing an untrusted draft must not become a way to perform accidental production writes.

### 14.1 Preview endpoint

Recommended API:

```text
POST /harness/ai/sessions/{sessionKey}/preview
```

The preview service executes the active draft with a preview-specific capability policy.

### 14.2 Preview capability policy

Default policy:

- `READ`: allowed subject to the current user's normal RuoYi permission
- `WRITE`: denied in preview
- `SENSITIVE_WRITE`: denied in preview
- `ADMIN`: denied in preview

A denied write capability should return a stable preview error that can be displayed in diagnostics.

Do not silently mock successful writes unless a future explicit mock-capability framework is added.

This ensures viewing AI-generated UI cannot mutate business data.

### 14.3 Preview pinning

Preview is pinned to the active draft version ID during that preview session. Regeneration creates/updates the draft and causes a deliberate preview reload.

---

## 15. Publish model

AI generation and validation never imply publication.

Publication remains an explicit user operation:

```text
Generate -> Validate -> Preview -> Publish
```

Requirements:

- existing `harness:app:publish` permission is authoritative,
- the target must be `VALIDATED`,
- server-side validation must have succeeded for the exact source hash being published,
- publish uses existing Phase 1 transactional publication service,
- Builder UI shows the exact version about to be published,
- AI provider is not called as part of runtime publication.

---

## 16. AI Builder API

Recommended endpoints:

```text
GET    /harness/ai/status
GET    /harness/ai/sessions
POST   /harness/ai/sessions
GET    /harness/ai/sessions/{sessionKey}
POST   /harness/ai/sessions/{sessionKey}/messages
POST   /harness/ai/sessions/{sessionKey}/preview
POST   /harness/ai/sessions/{sessionKey}/validate
POST   /harness/ai/sessions/{sessionKey}/publish
POST   /harness/ai/sessions/{sessionKey}/archive
```

### Create session

```json
{
  "appKey": null,
  "title": "Customer dashboard"
}
```

To edit an existing app:

```json
{
  "appKey": "customer-dashboard",
  "title": "Improve customer dashboard"
}
```

### Send user message

```json
{
  "message": "Add a customer-level filter and a refresh button."
}
```

Response:

```json
{
  "sessionKey": "...",
  "assistantMessage": "Added the filter and refresh action.",
  "appKey": "customer-dashboard",
  "versionId": 52,
  "versionNo": 8,
  "source": "defineApp({...});",
  "validation": {
    "valid": true,
    "diagnostics": [],
    "sourceHash": "sha256:..."
  },
  "capabilitiesUsed": [
    "example.customer.list"
  ]
}
```

---

## 17. Permissions

Add explicit Builder permissions:

```text
harness:ai:use
harness:ai:session:list
harness:ai:session:view
```

App/version mutation continues to require the existing permissions:

```text
harness:app:create
harness:app:edit
harness:app:validate
harness:app:publish
```

The Builder must not become a privilege escalation path.

Example:

- a user with `harness:ai:use` but without `harness:app:publish` may generate and preview drafts but cannot publish,
- a user lacking a business capability permission cannot gain that permission by asking the model to call it.

---

## 18. Frontend implementation

Add a stable RuoYi component key:

```text
harness/ai/builder
```

Recommended shared UI module:

```text
harness-ui/src/ai/
├─ AiBuilderView.vue
├─ ConversationPanel.vue
├─ PreviewPanel.vue
├─ BuilderToolbar.vue
├─ CapabilityUsagePanel.vue
└─ DiagnosticsPanel.vue
```

### Required UI states

The Builder must visibly represent:

- idle
- generating
- generation failed
- draft generated
- validation failed
- validation passed
- preview loading
- preview error
- unpublished changes
- publishing
- published

Do not hide validation failures behind generic toast messages. Show diagnostic code, message and line/column where available.

---

## 19. Model failure behavior

LLM failures are authoring failures, not runtime failures.

Handle separately:

- provider unavailable
- timeout
- authentication error
- rate limit
- malformed structured output
- generated invalid Harness script
- invented capability
- context too large

Return stable Harness AI error codes, for example:

```text
AI_DISABLED
AI_PROVIDER_UNAVAILABLE
AI_PROVIDER_AUTH_FAILED
AI_RATE_LIMITED
AI_TIMEOUT
AI_RESPONSE_INVALID
AI_SCRIPT_INVALID
AI_CAPABILITY_UNKNOWN
AI_CONTEXT_TOO_LARGE
```

Do not leak provider stack traces or API keys.

---

## 20. Automatic repair loop

The Builder may perform a bounded repair loop when generated code fails Harness validation.

Recommended maximum: **2 repair attempts** per user turn.

Flow:

```text
model generates script
        |
        v
Harness validation
        |
    invalid?
     /    \
   yes     no
    |       |
feed diagnostics
back to model
    |
regenerate
```

Rules:

- every attempt is bounded,
- the user must still see final diagnostics when repair fails,
- no publish occurs automatically,
- do not retry provider/rate-limit/authentication errors as script-repair attempts,
- do not create a new version row for every failed internal repair attempt; persist the final generated draft and optionally audit attempts separately.

---

## 21. Capability discovery UX

The AI Builder should expose what the AI can actually use.

Add a side panel or expandable section containing:

```text
Capabilities used by this draft

✓ example.customer.list       READ
✓ system.dict.list            READ
! crm.customer.create         WRITE (disabled in preview)
```

This makes generated applications understandable and debuggable.

The model's declared `capabilitiesUsed` list is advisory only. The server should also be able to derive actual capability calls from static/simple script inspection and runtime audit where practical.

Never treat the model's declaration as an authorization control.

---

## 22. AI configuration administration

Phase 2 should support environment-first configuration.

Do not build a complex multi-provider management platform.

An optional simple status page may show:

```text
AI Builder: Enabled
Provider: openai-compatible
Model: configured-model-name
Endpoint: configured / not configured
API Key: configured / not configured
```

Never return the actual API key.

A future phase may add multiple model profiles; Phase 2 does not require it.

---

## 23. Security requirements

AI-generated code is untrusted exactly like manually entered scripts.

Mandatory rules:

1. model output always passes the existing Script Runtime sandbox,
2. model output always passes normal UI schema validation,
3. capabilities always pass normal RuoYi permission checks,
4. AI cannot supply trusted user identity,
5. AI cannot publish without authenticated publication permission,
6. preview denies write/admin capabilities by default,
7. provider credentials never enter prompts or browser payloads,
8. user prompts and business schemas may contain sensitive information; logging must be configurable/redacted,
9. prompt injection inside business data/capability descriptions must not grant extra runtime authority,
10. the server treats `capabilitiesUsed` and all model explanations as untrusted metadata.

The strongest guarantee remains the Phase 1 execution boundary, not prompt instructions.

---

## 24. Tests

### 24.1 Backend unit tests

Add tests for:

- context assembly includes SDK/UI contract
- capability catalog projection excludes handlers/internal objects
- structured AI response parsing
- malformed AI response rejection
- unknown capability declaration rejection
- generation creates/updates only mutable drafts
- editing a published app creates a new draft
- validation diagnostics are returned to Builder
- bounded automatic repair
- provider timeout/rate-limit/auth errors map to stable error codes
- preview allows READ capabilities
- preview denies WRITE/SENSITIVE_WRITE/ADMIN capabilities
- Builder cannot publish without RuoYi publish permission
- AI disabled leaves Phase 1 runtime fully functional

### 24.2 Frontend tests

Add tests for:

- create Builder session
- send requirement
- render assistant response
- display generated source
- display validation diagnostics
- render preview using shared Harness renderer
- iterative modification
- unpublished change indication
- publish button permission/state behavior
- provider error state

### 24.3 End-to-end acceptance test

A complete Phase 2 acceptance scenario:

```text
1. Administrator starts normal RuoYi-Harness.
2. AI provider is configured.
3. User opens Harness -> AI Builder.
4. User enters:
   "Create a customer dashboard that lists customer name and level and has a refresh button."
5. Builder discovers `example.customer.list`.
6. AI returns Harness JavaScript.
7. Server saves it as a draft.
8. Existing Harness validator validates it.
9. Preview renders Acme / Globex through the normal Harness renderer.
10. User says:
    "Change the title to Customer Center and show total customer count above the table."
11. Builder updates the same mutable draft and validates again.
12. Preview reflects the change without RuoYi restart or frontend rebuild.
13. User clicks Publish.
14. Published app becomes available through its normal RuoYi dynamic menu.
15. A second generation creates a new draft, not a mutation of the published source.
16. Existing execution/capability audit still records runtime activity.
```

---

## 25. Definition of done

Phase 2 is complete when a normal user with appropriate RuoYi permissions can create and iteratively modify a Harness application **without manually writing JavaScript**:

```text
Natural language
    -> AI generation
    -> immutable/mutable version discipline
    -> Harness validation
    -> safe preview
    -> explicit publication
    -> normal Harness runtime
```

The user must not need to:

- edit Java source,
- build a JAR,
- build Vue code,
- restart the JVM,
- deploy a container,
- understand GraalVM,
- manually construct the UI JSON tree.

Developers may still inspect and manually edit the generated Harness script when needed.

The architecture remains deliberately small:

> **RuoYi is the host. Harness Runtime executes restricted scripts. AI Builder authors those scripts.**

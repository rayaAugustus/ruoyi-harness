# RuoYi Harness — Development Specification

## 1. Product outcome

Implement a production-oriented RuoYi extension that lets administrators create, validate, publish, hot-update and roll back **script applications** without restarting the host.

A published script application can:

- render a dynamic page using approved UI components
- call typed host capabilities exposed from Spring services
- handle named page actions
- reuse RuoYi authentication and permission checks
- emit auditable execution traces

The final application remains one normal RuoYi deployment.

---

## 2. Final functional surface

### Administration

The Harness administration console must provide:

- application list
- create/edit app metadata
- enable/disable app
- version history
- script editor
- syntax/runtime validation
- diagnostics display
- publish
- rollback
- source diff between versions
- execution logs
- capability invocation logs
- capability catalog viewer

### Runtime user experience

Users must be able to:

- see permitted dynamic apps in RuoYi navigation
- open `/harness/app/{appKey}`
- receive the currently published page
- interact with approved dynamic components
- execute named actions
- receive validation/permission/runtime errors in a controlled UI

### Java developer experience

Java developers must be able to expose a Spring-backed business operation as a Harness capability with a small amount of code.

Conceptual example:

```java
@HarnessCapability(
    name = "crm.customer.search",
    permission = "crm:customer:list",
    risk = RiskLevel.READ
)
public CustomerPage search(CustomerSearchInput input, CapabilityContext context) {
    return customerService.search(input);
}
```

Annotation usage is optional internally; the stable abstraction is the capability registry.

---

## 3. Backend components

Implement these logical components.

### 3.1 `harness-api`

Contains dependency-light contracts:

```text
AppDescriptor
AppVersionDescriptor
PageDefinition
UI component DTOs/schema types
ActionRequest/ActionResult
HarnessError
CapabilityDefinition
CapabilityContext
CapabilityHandler
RiskLevel
HarnessScriptEngine interface
ScriptExecutionContext
ScriptExecutionResult
```

Avoid dependencies on concrete RuoYi security implementations in this module.

### 3.2 `harness-core`

Contains:

- app registry service
- version service
- validation orchestration
- publication/rollback service
- cache management
- policy/limit configuration
- execution log orchestration

### 3.3 `harness-runtime`

Contains:

- JavaScript engine adapter
- restricted context builder
- SDK injection
- script parser/compiler cache
- execution timeout enforcement
- JSON boundary serialization
- action resolution
- diagnostics

No RuoYi-specific permission logic belongs here.

### 3.4 `harness-capability`

Contains:

- capability registry
- registration adapters
- schema validation
- invocation bridge
- risk/permission metadata
- capability audit hooks

### 3.5 `harness-ruoyi-adapter`

Contains all integration points to RuoYi:

- current authenticated user adapter
- permission evaluator adapter
- RuoYi menu synchronization
- audit/operator metadata
- host response conventions
- optional tenant context adapter if upstream edition supports tenancy

Do not scatter RuoYi-specific APIs across runtime/core modules.

---

## 4. Frontend components

### 4.1 Dynamic app view

Stable Vue route component:

```text
HarnessDynamicAppView
```

Responsibilities:

- resolve `appKey`
- request page definition
- show loading/error states
- render returned definition
- retain immutable `versionId` while page is open
- invoke actions using that version ID
- reload when user explicitly refreshes/navigates or server indicates version conflict

### 4.2 Renderer

Implement a registry mapping known schema component names to local Vue components.

Conceptual:

```typescript
const componentRegistry = {
  page: HarnessPage,
  section: HarnessSection,
  text: HarnessText,
  statistic: HarnessStatistic,
  table: HarnessTable,
  form: HarnessForm,
  input: HarnessInput,
  select: HarnessSelect,
  button: HarnessButton,
  tabs: HarnessTabs,
  modal: HarnessModal,
  alert: HarnessAlert,
  chart: HarnessChart,
};
```

Never resolve arbitrary component names from the script into Vue's global component space.

### 4.3 Client state

Dynamic form values and component interaction state live in the browser while a page is open.

An action may reference form/component state. The browser resolves those references into plain JSON input before sending the action request.

The script does not directly execute in the browser in the default architecture.

### 4.4 Admin console

Use the existing RuoYi UI stack and conventions where possible.

Pages:

```text
Harness / Applications
Harness / Application Detail
Harness / Script Editor
Harness / Versions
Harness / Execution Logs
Harness / Capabilities
```

The editor should support at least:

- JavaScript syntax highlighting
- validation action
- diagnostics with line/column
- save draft
- publish
- version history/diff

Monaco Editor is acceptable if its bundle impact is acceptable; otherwise use a lighter editor with JavaScript syntax support.

---

## 5. Script SDK v1

Freeze one coherent SDK rather than continuously adding DSL magic.

Minimum primitives:

```javascript
defineApp(...)
harness.call(name, input)
harness.context
harness.log.debug/info/warn/error

page(...)
section(...)
text(...)
statistic(...)
table(...)
form(...)
input(...)
select(...)
button(...)
tabs(...)
modal(...)
alert(...)
chart(...)
```

The helper functions construct plain page-definition objects. They must not give direct host access.

---

## 6. Capability SDK

Provide two registration paths.

### Programmatic registration

```java
registry.register(CapabilityDefinition.builder()
    .name("crm.customer.search")
    .version("1")
    .inputSchema(...)
    .outputSchema(...)
    .requiredPermission("crm:customer:list")
    .riskLevel(RiskLevel.READ)
    .handler(...)
    .build());
```

### Annotation convenience adapter

```java
@HarnessCapability(...)
```

The annotation scanner must translate annotated Spring methods into normal registry definitions.

Input/output should be normal DTOs converted through Jackson and validated at the boundary.

---

## 7. Persistence implementation

Create migrations/SQL for:

- `harness_app`
- `harness_app_version`
- `harness_execution_log`
- `harness_capability_log`

Follow the upstream project's supported database conventions. Keep SQL dialect dependencies isolated where possible.

Required indexes include:

```text
harness_app(app_key unique)
harness_app_version(app_id, version_no unique)
harness_execution_log(trace_id)
harness_execution_log(app_id, started_at)
harness_capability_log(trace_id)
harness_capability_log(capability_name, created_at)
```

Do not create a separate database per dynamic app.

---

## 8. Publication semantics

### Draft editing

- source may change while DRAFT
- saving draft does not affect active users

### Validation

- parse/static checks
- restricted validation execution
- produce diagnostics
- successful validation moves version to VALIDATED
- changing source after validation returns it to DRAFT

### Publish

- requires publish permission
- target must be VALIDATED
- publication transaction atomically switches the app pointer
- invalidate relevant caches
- published source becomes immutable

### Concurrent users

Open pages are pinned to the immutable `versionId` they rendered with. Their actions continue to use that version until reload/navigation.

New page loads resolve the newly published version.

### Rollback

Rollback atomically switches the publication pointer to a selected previous validated/published immutable version.

No source rewriting occurs.

---

## 9. Runtime engine implementation rules

### Context creation

Create a restricted context per execution or use a rigorously resettable pool only after correctness is proven.

Correctness and isolation are more important than premature pooling.

### No imports by default

SDK v1 scripts should be self-contained. Do not allow arbitrary `import`/`require` of filesystem or package modules.

If modular scripts are added later, resolve only app-owned immutable virtual modules from the Harness repository/database.

### Async behavior

Support the minimum async semantics needed for `harness.call`.

Do not expose general timers/background scheduling in SDK v1.

### Deterministic boundary

Before leaving runtime, recursively convert results into safe JSON-compatible values with limits on depth, collection size and total serialized bytes.

---

## 10. UI renderer rules

Each component must have:

- TypeScript type
- JSON Schema definition
- backend validation coverage
- frontend renderer implementation
- safe default behavior

Unknown fields should either be rejected or ignored according to one documented schema policy. Prefer rejecting unknown structural fields during validation so generated scripts fail early.

No component may accept arbitrary executable JavaScript.

---

## 11. Example built-in capabilities

The repository should ship a small set of safe system capabilities to demonstrate the contract:

```text
system.user.current
system.dict.options
system.time.now
```

And one example business domain under `examples/`, such as customer management:

```text
example.customer.list
example.customer.get
example.customer.create
example.customer.update
```

This example exists to verify the architecture end to end; it must use the same public capability APIs as external business modules.

---

## 12. Observability

Use one `traceId` across:

```text
HTTP runtime request
script execution
all capability calls
runtime response
```

Record elapsed time and status for script and capability stages.

Expose admin filters by:

- trace ID
- app
- version
- user
- capability
- status
- time range

Do not introduce a separate observability stack as a hard dependency. Integrate with normal application logging and database audit records; allow future OpenTelemetry integration behind adapters.

---

## 13. Configuration

Recommended configuration namespace:

```yaml
harness:
  enabled: true
  runtime:
    engine: graaljs
    max-execution-millis: 3000
    max-capability-calls: 50
    max-input-bytes: 262144
    max-output-bytes: 1048576
    max-page-nodes: 2000
    max-log-events: 200
  cache:
    enabled: true
  audit:
    enabled: true
```

Numbers above are starting defaults, not protocol guarantees. Keep them configurable and covered by tests.

---

## 14. Test specification

### Unit tests

Required for:

- app/version state transitions
- publication transaction rules
- capability registry collision behavior
- input/output schema validation
- permission evaluation
- UI schema validation
- JSON boundary limits
- error mapping

### Runtime security tests

Implement every scenario listed in `docs/SECURITY.md`.

### Integration tests

Required flows:

#### Create and publish

```text
create app
 -> create draft
 -> validate
 -> publish
 -> render page
```

#### Hot update

```text
render v1
 -> create/validate/publish v2
 -> new render receives v2
 -> no JVM restart
```

#### Version pinning

```text
open v1 page
 -> publish v2
 -> v1 page action executes against v1
 -> reload receives v2
```

#### Rollback

```text
publish v1
 -> publish v2
 -> rollback v1
 -> new render receives v1
```

#### Permission denial

```text
script calls protected capability
 -> current user lacks permission
 -> handler is not executed
 -> audited denial
```

#### Disabled app

```text
disable app
 -> render/action denied
 -> existing source/version remains intact
```

### Frontend tests

Test:

- approved component rendering
- unknown component rejection/error UI
- form state -> action input
- action effects
- version pin propagation
- sanitized text rendering

---

## 15. Implementation sequence

This is a dependency order for building the complete system, not a change in product scope.

1. Establish module/package boundaries and shared contracts.
2. Add database tables/repositories and app/version state model.
3. Implement publication and rollback transaction semantics.
4. Implement capability registry and RuoYi permission adapter.
5. Implement restricted JavaScript engine abstraction and GraalJS adapter.
6. Implement SDK injection and safe JSON boundary.
7. Implement page/action runtime HTTP APIs.
8. Define machine-readable UI schemas and backend validators.
9. Implement Vue dynamic renderer and stable app route.
10. Implement RuoYi menu synchronization.
11. Implement administration console and script editor.
12. Implement execution/capability audit views.
13. Add complete sandbox/security regression suite.
14. Add example customer app/capabilities and end-to-end tests.
15. Optimize immutable script compilation/cache only after correctness tests pass.

Do not replace this architecture with microservices or code generation during implementation.

---

## 16. Acceptance scenario

The following scenario must work on one running RuoYi instance:

1. Administrator creates app `customer-dashboard`.
2. Administrator creates a script version that calls `example.customer.list` and returns a table page.
3. Validation succeeds.
4. Administrator publishes it.
5. A permitted user sees the app menu and opens it.
6. The table renders with data returned by the Spring-backed capability.
7. A button invokes a named script action.
8. Every capability call checks the user's RuoYi permission.
9. Administrator edits a new draft and publishes v2.
10. New page loads immediately use v2 with no process restart and no frontend rebuild.
11. Administrator rolls back to v1.
12. New page loads immediately use v1.
13. Execution and capability logs show the correct user, app, immutable version and trace ID.
14. Sandbox regression tests prove the script cannot access JVM, filesystem, OS process or arbitrary network resources.

If this complete scenario works, the core product architecture has been implemented correctly.
# RuoYi Harness — System Architecture

## 1. Purpose

RuoYi Harness is a **single deployable RuoYi application** that can host dynamic business applications written as scripts.

The project deliberately avoids independently deployed microservices, Docker/Kubernetes application modules, and dynamic JAR loading. Dynamic functionality is represented as versioned script applications interpreted by a restricted runtime inside the host.

The architectural goal is **hot-pluggable functionality without host restart** while keeping authority and persistence inside normal RuoYi/Spring services.

---

## 2. High-level architecture

```text
┌──────────────────────────────── Browser ────────────────────────────────┐
│                                                                         │
│  RuoYi Shell                                                            │
│  ├─ Login / layout / navigation                                         │
│  ├─ Dynamic App Router                                                  │
│  ├─ Dynamic UI Renderer                                                 │
│  └─ Harness Admin Console                                               │
│                                                                         │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │ HTTPS / JSON
                                ▼
┌──────────────────────────── RuoYi Host ─────────────────────────────────┐
│                                                                         │
│  Existing RuoYi capabilities                                            │
│  ├─ Authentication                                                      │
│  ├─ Users / Roles / Permissions                                         │
│  ├─ Menus                                                               │
│  ├─ Audit                                                               │
│  └─ Spring business services                                            │
│                                                                         │
│  Harness                                                                │
│  ├─ App Registry                                                        │
│  ├─ Version & Publication Manager                                       │
│  ├─ Script Runtime                                                      │
│  ├─ Capability Registry / Bridge                                        │
│  ├─ Runtime Policy Guard                                                │
│  ├─ Route/Menu Integration                                              │
│  └─ Execution & Capability Audit                                        │
│                                                                         │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────── Persistence ────────────────────────────────┐
│                                                                         │
│  RuoYi database + Harness metadata tables                               │
│  Optional Redis cache for published artifacts and runtime metadata       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

There is **one host application**. A dynamic app is not a process or JVM module. It is data: manifest, script source, version metadata and permissions.

---

## 3. Runtime request flow

When a user opens `/harness/app/customer`:

```text
1. Browser route /harness/app/:appKey
2. Dynamic App Router requests active app descriptor
3. Host resolves:
   - app
   - published version
   - current user permissions
4. Host executes the app entry script in the restricted Script Runtime
5. Script may use the Capability Bridge to request allowed host data/actions
6. Script returns a JSON-compatible Page Definition
7. Host validates/normalizes the Page Definition
8. Browser Dynamic UI Renderer renders the page using approved Vue components
9. UI events call typed Harness action endpoints
10. Action execution passes through permission/policy/audit checks
```

A publish changes only the version pointer. New page requests immediately resolve the new version. No RuoYi restart is required.

---

## 4. Core components

### 4.1 App Registry

Owns dynamic application identity and metadata.

Responsibilities:

- app key and display name
- route metadata
- icon/order/menu metadata
- enabled/disabled state
- required app-level permissions
- current published version pointer
- ownership/creator metadata

It does **not** execute code.

### 4.2 Version & Publication Manager

Maintains immutable versions.

Lifecycle:

```text
DRAFT -> VALIDATED -> PUBLISHED
                  \-> REJECTED
PUBLISHED -> SUPERSEDED
PUBLISHED/SUPERSEDED -> ROLLED_BACK_TO (publication pointer operation)
```

Rules:

- published source is immutable
- every edit creates or modifies a draft
- validation happens before publish
- rollback moves the publication pointer to a previous immutable version
- disable is app state, not source deletion

### 4.3 Script Runtime

Executes app scripts in an isolated, restricted JavaScript context.

Responsibilities:

- compile/parse script
- inject a minimal Harness SDK
- enforce time/resource limits
- prevent host/JVM access
- produce deterministic JSON-compatible results
- collect runtime diagnostics

The Script Runtime must never expose arbitrary Java host access.

### 4.4 Capability Registry

A registry of business operations that scripts are allowed to invoke.

Each capability is described by:

```text
name
version
description
input schema
output schema
required permission
risk level
handler
```

Examples:

```text
system.user.current
crm.customer.search
crm.customer.create
report.sales.monthly
```

Capabilities wrap normal Spring services. Scripts never receive repositories, mappers or database connections.

### 4.5 Capability Bridge

The only privileged bridge between script and host.

Conceptual API:

```javascript
const result = await harness.call("crm.customer.search", {
  keyword: "Acme",
  page: 1,
  size: 20
});
```

Server execution path:

```text
script
 -> bridge
 -> capability lookup
 -> schema validation
 -> permission check
 -> policy/risk check
 -> handler invocation
 -> output validation
 -> audit
 -> script
```

### 4.6 Runtime Policy Guard

Enforces cross-cutting limits:

- execution timeout
- maximum capability calls per execution
- request payload size
- response size
- allowed capability set
- risk-based confirmation policy where applicable
- tenant/user context consistency

### 4.7 Dynamic UI Renderer

A Vue component tree renderer on the browser side.

The server-side script returns **Page Definition JSON**, not HTML and not arbitrary DOM-manipulating JavaScript.

Approved component types can include:

- page
- section
- text
- statistic
- table
- form
- input
- select
- button
- tabs
- modal
- alert
- chart

Each component has a strict schema. Unknown component types or properties are rejected.

### 4.8 Dynamic App Router

A stable host route loads dynamic applications:

```text
/harness/app/:appKey
```

The system does not need to rebuild the Vue application when an app is published.

RuoYi menus point to this stable route with the app key as route metadata/parameter.

---

## 5. Separation of responsibilities

### RuoYi host owns

- login/session/token
- users
- roles
- permissions
- menus
- Spring services
- database access
- transaction boundaries
- audit persistence
- application configuration

### Harness owns

- dynamic app registry
- script versions
- script execution
- capability contract
- page definition contract
- hot publication/rollback
- dynamic route integration
- runtime guardrails

### Script app owns

- page composition
- presentation logic
- orchestration of allowed capabilities
- local ephemeral state declarations
- event/action bindings

### Script app must never own

- raw SQL
- JDBC/DataSource access
- filesystem access
- OS process access
- arbitrary outbound networking
- Java reflection/class loading
- authentication decisions
- permission bypasses
- host configuration mutation

---

## 6. Recommended package/module layout

The exact upstream RuoYi structure may vary. Keep Harness integration isolated.

```text
ruoyi-harness/
├─ AGENTS.md
├─ docs/
├─ harness-backend/
│  ├─ harness-api/              # DTOs, schemas, public interfaces
│  ├─ harness-core/             # registry, publication, policy
│  ├─ harness-runtime/          # JS engine abstraction and implementation
│  ├─ harness-capability/       # capability registry/bridge
│  └─ harness-ruoyi-adapter/    # auth/menu/audit/RuoYi integration
├─ harness-ui/
│  ├─ renderer/                 # dynamic UI renderer
│  ├─ runtime-client/           # API client
│  └─ admin/                    # app/script/version administration
└─ examples/
   └─ customer-app/
```

If integrated directly into an existing RuoYi repository, preserve the same logical boundaries even if physical Maven modules differ.

---

## 7. Hot-plug behavior

“Hot-plug” in this project means:

- create app at runtime
- edit script at runtime
- validate at runtime
- publish at runtime
- disable/enable at runtime
- rollback at runtime
- delete an unused draft at runtime

None of these operations restart the host JVM.

Hot-plug is achieved by **versioned script data + runtime interpretation**, not dynamic Java class replacement.

---

## 8. Caching

Published app artifacts may be cached by `(appKey, versionId)`.

Recommended behavior:

```text
publish
 -> transaction commits new publication pointer
 -> invalidate app descriptor cache
 -> optionally precompile/warm script cache
```

Never use a cache key that omits the version ID for immutable compiled artifacts.

A publication pointer cache may use `appKey`, but must be invalidated atomically after publish/rollback.

---

## 9. Failure isolation

A dynamic script failure must degrade to an application-level error page, not crash the host.

Required boundaries:

- execution exceptions caught at runtime boundary
- timeout interrupts/cancels the execution
- capability exceptions converted to structured errors
- renderer receives only valid page definitions
- per-request execution state is disposable
- no script-created background threads

---

## 10. Architectural acceptance criteria

The architecture is complete when all of the following are true:

1. A new script app can be created while the host is running.
2. Publishing the app makes a new route/menu usable without host restart or frontend rebuild.
3. The script can render a page composed from approved components.
4. The script can invoke an explicitly registered Spring-backed capability.
5. A user without the capability permission is denied even if the script requests it.
6. Script code cannot access Java classes, filesystem, environment variables, process execution or arbitrary network APIs.
7. Publishing a new version changes subsequent page loads immediately.
8. Existing published versions remain immutable.
9. Rollback switches back to a previous version without restart.
10. Runtime execution and capability calls are traceable in audit logs.
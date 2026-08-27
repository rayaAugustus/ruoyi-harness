# RuoYi Harness

> Hot-loadable script applications for the RuoYi ecosystem.

RuoYi Harness turns a normal RuoYi application into a host for **runtime-loadable business pages and workflows implemented as restricted JavaScript scripts**.

The host remains a normal single RuoYi deployment. Dynamic applications are stored as versioned script data and can be created, validated, published, updated, disabled and rolled back **without restarting the JVM and without rebuilding the frontend**.

## What it is

```text
User
  |
  v
RuoYi Web Shell
  |
  v
Dynamic App Renderer
  |
  v
Harness Script Runtime
  |
  +---- restricted JavaScript
  |
  v
Capability Bridge
  |
  v
RuoYi / Spring Business Services
  |
  v
Database
```

A script can compose approved UI components and call explicitly exported host capabilities:

```javascript
defineApp({
  page: async () => {
    const customers = await harness.call("crm.customer.search", {
      page: 1,
      size: 20
    });

    return page({
      title: "Customers",
      children: [
        table({
          id: "customers",
          rows: customers.items,
          columns: [
            { key: "name", label: "Name" },
            { key: "level", label: "Level" }
          ]
        })
      ]
    });
  }
});
```

Publishing a new script version changes subsequent page loads immediately. No JAR reload, Docker container, Kubernetes workload, microservice deployment, or host restart is involved.

## Phase 2 — AI Builder

Phase 1 implemented the runtime, versioning, capability bridge, security boundary, administration UI and dynamic renderer.

The active development phase is now **AI Builder**:

```text
Natural language requirement
    |
    v
AI Builder
    |
    +-- Harness SDK contract
    +-- available capabilities
    +-- current app/draft
    |
    v
Generated Harness script
    |
    v
Validate -> Preview -> Explicit Publish
    |
    v
Existing Harness Runtime
```

The intended user experience becomes:

> Describe the business page you want, let AI author the Harness script, preview it, ask for changes, then publish it into RuoYi.

AI Builder is an authoring layer only. Published applications continue to run independently of any LLM provider.

See [`docs/PHASE2_AI_BUILDER.md`](./docs/PHASE2_AI_BUILDER.md) for the complete Phase 2 development specification.

## Design boundaries

RuoYi Harness is intentionally **not**:

- a Spring Cloud microservice platform
- Docker/Kubernetes orchestration
- an OSGi/PF4J dynamic JAR loader
- arbitrary server-side JavaScript execution
- a general-purpose low-code operating system
- a replacement for RuoYi authentication/RBAC
- an autonomous enterprise Agent OS

RuoYi remains responsible for identity, permissions, business services and persistence. Scripts are untrusted and can access host functionality only through a typed, permission-checked Capability Bridge.

## Core properties

- **Hot publication** — publish/update/rollback scripts without host restart.
- **Restricted runtime** — JavaScript runs in a deny-by-default sandbox.
- **Declarative UI** — scripts return validated JSON page definitions rendered by Vue.
- **Typed capabilities** — Spring services are exposed through explicit schemas and permissions.
- **RuoYi-native security** — existing users, roles and permissions remain authoritative.
- **Immutable versions** — published versions never mutate; publication is a pointer switch.
- **Auditable execution** — every script execution and capability call carries a trace ID.
- **AI-authored, runtime-independent** — AI can generate/edit scripts, while normal runtime execution remains independent of an LLM.

## Architecture documents

Coding agents should start with [`AGENTS.md`](./AGENTS.md), then read:

1. [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) — final system boundaries and request flow.
2. [`docs/SCRIPT_RUNTIME.md`](./docs/SCRIPT_RUNTIME.md) — JavaScript runtime, SDK and action model.
3. [`docs/API_CONTRACT.md`](./docs/API_CONTRACT.md) — HTTP APIs, capability contract, UI schema and persistence model.
4. [`docs/SECURITY.md`](./docs/SECURITY.md) — sandbox and permission threat model.
5. [`docs/DEVELOPMENT_SPEC.md`](./docs/DEVELOPMENT_SPEC.md) — Phase 1 implementation and acceptance specification.
6. [`docs/PHASE2_AI_BUILDER.md`](./docs/PHASE2_AI_BUILDER.md) — active AI Builder implementation specification.

## Current implementation shape

```text
ruoyi-harness/
├─ AGENTS.md
├─ docs/
├─ RuoYi-Vue/
│  ├─ harness-api/
│  ├─ harness-core/
│  ├─ harness-runtime/
│  ├─ harness-capability/
│  ├─ harness-ai/
│  └─ harness-ruoyi-adapter/
├─ RuoYi-Vue3/
├─ harness-ui/
│  ├─ renderer/
│  ├─ runtime-client/
│  ├─ admin/
│  └─ ai/
├─ contracts/
└─ examples/
   └─ customer-app/
```

Phase 2 adds an isolated `harness-ai` authoring module and AI Builder UI. It must not make `harness-runtime` depend on an LLM provider.

## Running the implementation

1. Import the normal RuoYi schema, then import [`RuoYi-Vue/sql/harness.sql`](./RuoYi-Vue/sql/harness.sql). The latter creates the immutable app/version stores, both audit stores, and the Harness administration permissions/menu.
2. Configure the existing RuoYi datasource and Redis settings. Harness settings are under `harness` in `ruoyi-admin/src/main/resources/application.yml`; the default runtime is GraalJS and can be disabled as a unit with `harness.enabled=false`.
3. Build or run the host from `RuoYi-Vue`. `ruoyi-admin` already depends on `harness-ruoyi-adapter`, while the domain, runtime, capability bridge, and RuoYi integration remain separate Maven modules.
4. Install and run `RuoYi-Vue3`. It is already wired to the shared `harness-ui` source, reuses RuoYi's authenticated Axios transport, and implements every stable backend menu component key under `src/views/harness`.

AI Builder is disabled until a provider is configured. Credentials remain server-side:

```text
HARNESS_AI_ENABLED=true
HARNESS_AI_BASE_URL=https://provider.example/v1
HARNESS_AI_API_KEY=...
HARNESS_AI_MODEL=...
```

The initial adapter uses the OpenAI-compatible chat-completions contract. Its response must be strict JSON containing `assistantMessage`, `script`, and `capabilitiesUsed`. Importing `harness.sql` also installs the AI Builder menu, session/message tables, and `harness:ai:*` permissions. Preview permits only `READ` capabilities and still applies the current user's normal RuoYi permissions.

The concrete script entry style selected by this implementation is the single global `defineApp({...})` registration shown above. ES modules, `require`, host class lookup, filesystem, process, environment, raw networking, threads, and browser DOM access are rejected. See [`examples/customer-app/app.js`](./examples/customer-app/app.js) for an executable example and [`contracts/ui-schema-v1.json`](./contracts/ui-schema-v1.json) for the renderer contract.

Verification commands:

```text
cd RuoYi-Vue
mvn test

cd ../harness-ui
npm ci
npm test
npm run build
```

## Product completion direction

The product is moving from:

```text
Developer writes Harness JavaScript
    -> Runtime
    -> Dynamic RuoYi app
```

to:

```text
User describes software
    -> AI Builder writes Harness JavaScript
    -> Validate / Preview / Publish
    -> Runtime
    -> Dynamic RuoYi app
```

The runtime remains intentionally small. AI changes how applications are authored, not the security or execution model.

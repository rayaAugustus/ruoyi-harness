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
export default defineApp({
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

## Design boundaries

RuoYi Harness is intentionally **not**:

- a Spring Cloud microservice platform
- Docker/Kubernetes orchestration
- an OSGi/PF4J dynamic JAR loader
- arbitrary server-side JavaScript execution
- a general-purpose low-code operating system
- a replacement for RuoYi authentication/RBAC

RuoYi remains responsible for identity, permissions, business services and persistence. Scripts are untrusted and can access host functionality only through a typed, permission-checked Capability Bridge.

## Core properties

- **Hot publication** — publish/update/rollback scripts without host restart.
- **Restricted runtime** — JavaScript runs in a deny-by-default sandbox.
- **Declarative UI** — scripts return validated JSON page definitions rendered by Vue.
- **Typed capabilities** — Spring services are exposed through explicit schemas and permissions.
- **RuoYi-native security** — existing users, roles and permissions remain authoritative.
- **Immutable versions** — published versions never mutate; publication is a pointer switch.
- **Auditable execution** — every script execution and capability call carries a trace ID.
- **AI-friendly, AI-independent** — coding agents/LLMs can generate scripts, but runtime execution does not depend on an LLM.

## Architecture documents

Coding agents should start with [`AGENTS.md`](./AGENTS.md), then read:

1. [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) — final system boundaries and request flow.
2. [`docs/SCRIPT_RUNTIME.md`](./docs/SCRIPT_RUNTIME.md) — JavaScript runtime, SDK and action model.
3. [`docs/API_CONTRACT.md`](./docs/API_CONTRACT.md) — HTTP APIs, capability contract, UI schema and persistence model.
4. [`docs/SECURITY.md`](./docs/SECURITY.md) — sandbox and permission threat model.
5. [`docs/DEVELOPMENT_SPEC.md`](./docs/DEVELOPMENT_SPEC.md) — complete implementation and acceptance specification.

## Target implementation shape

```text
ruoyi-harness/
├─ AGENTS.md
├─ docs/
├─ harness-backend/
│  ├─ harness-api/
│  ├─ harness-core/
│  ├─ harness-runtime/
│  ├─ harness-capability/
│  └─ harness-ruoyi-adapter/
├─ harness-ui/
│  ├─ renderer/
│  ├─ runtime-client/
│  └─ admin/
└─ examples/
   └─ customer-app/
```

Recommended JavaScript implementation is GraalVM Polyglot JavaScript behind an engine abstraction. The final implementation must prove sandbox restrictions with security regression tests rather than relying only on configuration.

## Completion criterion

The architecture is complete when one running RuoYi instance can create a script app, validate it, publish it, render its page, invoke permission-checked Spring capabilities, publish a new version, hot-switch new users to it, roll back to an earlier immutable version, and audit the entire flow — without restarting the host.
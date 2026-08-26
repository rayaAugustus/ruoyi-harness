# RuoYi Harness — Agent Development Instructions

This repository is intended to be implemented with coding agents such as Codex. Treat the documents under `docs/` as the product and architecture specification.

## Product definition

RuoYi Harness is **a RuoYi-based application with a hot-loadable script application layer**. It is not a microservice platform, container orchestrator, low-code operating system, or general-purpose application OS.

The core idea is simple:

1. RuoYi remains the host application and supplies authentication, users, roles, permissions, menus, persistence, audit and normal Java business services.
2. A dynamic application is stored as script + manifest data, not as a JAR, Docker image, Kubernetes workload, or independently deployed service.
3. Opening a dynamic page loads the published script version and executes it in a restricted runtime.
4. The script produces a UI description and invokes only explicitly exposed host capabilities through a controlled bridge.
5. Updating/publishing a script takes effect without restarting the RuoYi host.
6. AI may generate or edit these scripts, but the runtime must work independently of any specific LLM provider.

## Current development phase

**Phase 1 runtime implementation exists. The active development target is Phase 2: AI Builder.**

Before implementing Phase 2, read:

- `docs/PHASE2_AI_BUILDER.md`

Phase 2 adds an authoring layer with this product flow:

```text
Natural language requirement
    -> AI Builder
    -> generated Harness script
    -> existing Harness validation
    -> safe preview
    -> explicit publish
    -> existing Harness runtime
```

Do not redesign the Phase 1 execution architecture while implementing the AI Builder.

Phase 2 must also preserve/fix these Phase 1 invariants:

1. rollback may target only previously published immutable versions, never a merely `VALIDATED` mutable version,
2. open pages remain pinned to their immutable version for internal action/refresh execution until navigation/reload,
3. concurrent publish/rollback operations must be serialized or protected with optimistic/CAS semantics.

## Non-goals

Do not turn this project into any of the following unless the specification is explicitly changed:

- Kubernetes/Docker orchestration
- Spring Cloud microservices
- dynamic Java/JAR class loading
- OSGi/PF4J plugin framework
- arbitrary server-side code execution
- a full low-code/BPM/ERP platform
- a second RBAC system competing with RuoYi
- direct database access from scripts
- direct JVM/Java class access from scripts
- an autonomous enterprise Agent OS
- automatic production publishing simply because AI generation succeeded

## Architectural invariants

The implementation must preserve these invariants:

- **Host owns authority.** Authentication, authorization, data access and privileged operations remain in Java/Spring services.
- **Scripts are untrusted.** A script can only call capabilities explicitly exported by the bridge.
- **No host restart for script changes.** Publish/rollback/disable must be runtime operations.
- **Published versions are immutable.** Editing creates a new draft/version; a published artifact is never mutated in place.
- **UI is declarative.** Scripts return a JSON-compatible page tree; they do not receive raw browser DOM access from the server runtime.
- **Capability calls are typed.** Every exposed host capability has a name, input schema, output schema, permission requirement and risk level.
- **Everything is auditable.** Script execution and capability invocation must carry user, app, version, request and trace context.
- **LLM is optional.** Runtime execution cannot depend on an LLM being available.
- **AI Builder is authoring only.** Model output always becomes an ordinary untrusted Harness draft and passes the existing sandbox, schema validation, permission checks and publication path.
- **Preview is safe by default.** Draft preview may use READ capabilities subject to normal permissions; WRITE, SENSITIVE_WRITE and ADMIN capabilities are denied by default.

## Source of truth

Read these before implementing:

1. `docs/ARCHITECTURE.md`
2. `docs/SCRIPT_RUNTIME.md`
3. `docs/API_CONTRACT.md`
4. `docs/SECURITY.md`
5. `docs/DEVELOPMENT_SPEC.md`
6. `docs/PHASE2_AI_BUILDER.md` — active Phase 2 product/development specification

When documents conflict, use this order of precedence:

`AGENTS.md` > `docs/ARCHITECTURE.md` > `docs/PHASE2_AI_BUILDER.md` for Phase 2 authoring behavior > `docs/SCRIPT_RUNTIME.md` > other docs.

The Phase 2 document may extend the system with AI-authoring APIs and persistence, but it may not weaken Phase 1 runtime/security invariants.

## Phase 2 engineering rules

- Implement AI functionality in a separate `harness-ai` module/package; do not make `harness-runtime` depend on an LLM SDK/provider.
- Start with a provider-independent model interface and an OpenAI-compatible HTTP adapter.
- Keep provider credentials server-side and externalized through environment/application configuration.
- Build compact model context from the Harness SDK contract, UI schema, safe capability catalog and current app/draft. Do not send the entire repository to the model.
- Require structured AI output containing at least `assistantMessage`, `script` and `capabilitiesUsed`.
- Never parse/execute arbitrary prose as JavaScript.
- Never let the LLM write directly to Harness database tables. Use the existing app/version services.
- Never let model-declared capabilities bypass the real `CapabilityRegistry` or permission checks.
- Generated source must pass the existing Harness validator before preview/publication.
- Bound automatic validation-repair loops; the Phase 2 spec recommends at most two repair attempts per user turn.
- Preview untrusted drafts through the same script engine/renderer, using a preview capability policy that denies write/admin operations by default.
- Publishing always requires explicit authenticated user intent and existing `harness:app:publish` authorization.
- AI provider failure must not affect already-published applications or normal Harness runtime.

## General engineering rules

- Keep the runtime small and explicit.
- Prefer interfaces and adapters over modification of RuoYi core code.
- Keep all RuoYi-specific integration behind a dedicated adapter/integration package.
- Do not expose Spring `ApplicationContext`, `DataSource`, MyBatis mappers, filesystem, network sockets, environment variables, reflection, process APIs, or Java host objects to scripts.
- Validate every cross-boundary value against schemas.
- Add tests for security boundaries, version switching, rollback, permission denial and timeout behavior.
- Every implementation PR should reference the relevant specification section and add/update tests.

## Phase 1 definition of done

The runtime layer is architecturally complete when an administrator can create an app, edit a script, validate it, publish it, assign permissions, open its route, render the generated page, invoke allowed host capabilities, observe audit logs, publish a new version, instantly switch new page loads to it, keep existing pages version-pinned, and roll back to a previous immutable version — all without restarting the RuoYi process.

## Phase 2 definition of done

Phase 2 is complete when a user with appropriate RuoYi permissions can create and iteratively modify a Harness app **without manually writing JavaScript**:

```text
Natural language
    -> AI generation
    -> Harness draft/version discipline
    -> validation
    -> safe preview
    -> explicit publish
    -> normal Harness runtime
```

The user must not need to build Java, build Vue, restart the host, deploy a container, or understand the GraalVM implementation. Developers must still be able to inspect and manually edit the generated script.
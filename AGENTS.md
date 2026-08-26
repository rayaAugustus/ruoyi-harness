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

## Source of truth

Read these before implementing:

1. `docs/ARCHITECTURE.md`
2. `docs/SCRIPT_RUNTIME.md`
3. `docs/API_CONTRACT.md`
4. `docs/SECURITY.md`
5. `docs/DEVELOPMENT_SPEC.md`

When documents conflict, use this order of precedence:

`AGENTS.md` > `docs/ARCHITECTURE.md` > `docs/SCRIPT_RUNTIME.md` > other docs.

## Engineering rules

- Keep the runtime small and explicit.
- Prefer interfaces and adapters over modification of RuoYi core code.
- Keep all RuoYi-specific integration behind a dedicated adapter/integration package.
- Do not expose Spring `ApplicationContext`, `DataSource`, MyBatis mappers, filesystem, network sockets, environment variables, reflection, process APIs, or Java host objects to scripts.
- Validate every cross-boundary value against schemas.
- Add tests for security boundaries, version switching, rollback, permission denial and timeout behavior.
- Every implementation PR should reference the relevant specification section and add/update tests.

## Definition of done

The final system is considered architecturally complete when an administrator can create an app, edit a script, validate it, publish it, assign permissions, open its route, render the generated page, invoke allowed host capabilities, observe audit logs, publish a new version, instantly switch users to it, and roll back to the previous version — all without restarting the RuoYi process.
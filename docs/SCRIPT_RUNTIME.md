# RuoYi Harness — Script Runtime Specification

## 1. Runtime model

A dynamic application is a versioned JavaScript program executed by the RuoYi host in a restricted runtime.

The runtime must be abstracted behind an interface so the JavaScript engine can be replaced without changing application contracts.

Recommended interface:

```java
public interface HarnessScriptEngine {
    ValidationResult validate(ScriptArtifact artifact);
    ScriptExecutionResult execute(ScriptArtifact artifact, ScriptExecutionContext context);
}
```

Recommended implementation technology: **GraalVM Polyglot JavaScript** (GraalJS), configured with host access disabled by default.

Do not couple business code directly to GraalVM APIs. Put engine-specific code behind `HarnessScriptEngine`.

---

## 2. Script artifact

A script version contains at minimum:

```text
id
appId
version
source
sourceHash
status
createdBy
createdAt
validatedAt
publishedAt
```

A script artifact is immutable after publication.

The source is UTF-8 text. The runtime may maintain a compiled/preparsed cache keyed by immutable version ID and source hash.

---

## 3. Script entry contract

Each application script must export or return an application definition through one stable entry point.

Preferred contract:

```javascript
export default defineApp({
  page: async (ctx) => {
    return page({
      title: "Customers",
      children: []
    });
  },
  actions: {
    async refresh(ctx, input) {
      return { refresh: true };
    }
  }
});
```

If the chosen engine integration makes ES modules inconvenient, an equivalent single global registration form is acceptable:

```javascript
defineApp({ ... });
```

Do not support multiple competing entry styles in the final API.

---

## 4. Injected SDK

Scripts receive only a narrow SDK. Conceptually:

```typescript
interface HarnessSDK {
  call<T = unknown>(capability: string, input?: unknown): Promise<T>;
  ui: UIBuilder;
  context: Readonly<RuntimeContext>;
  log: SafeLogger;
}
```

The actual implementation may expose ergonomic top-level helpers such as `page`, `table`, `form` and `button`, but they must map to the same validated UI schema.

### Runtime context

Read-only context may expose:

```typescript
interface RuntimeContext {
  app: {
    key: string;
    version: string;
  };
  user: {
    id: string;
    name: string;
  };
  locale?: string;
  requestId: string;
  traceId: string;
}
```

Do not expose authentication tokens, password hashes, raw security objects or unrestricted session internals.

---

## 5. Capability invocation

Script example:

```javascript
export default defineApp({
  page: async () => {
    const customers = await harness.call("crm.customer.search", {
      keyword: "",
      page: 1,
      size: 20
    });

    return page({
      title: "Customer Management",
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

Every call flows through the server Capability Bridge. No script may directly construct Spring beans or query the database.

---

## 6. UI definition contract

The script result must be JSON-compatible. Functions, Java objects, class instances, cyclic structures and arbitrary prototype graphs cannot cross the runtime boundary.

Example:

```json
{
  "type": "page",
  "title": "Customer Management",
  "children": [
    {
      "type": "table",
      "id": "customers",
      "columns": [
        { "key": "name", "label": "Name" },
        { "key": "level", "label": "Level" }
      ],
      "rows": []
    }
  ]
}
```

The server validates this result before sending it to the browser.

---

## 7. Events and actions

Do not serialize JavaScript callbacks into the browser.

Instead, components bind events to named server-side script actions:

```javascript
button({
  text: "Create Customer",
  action: {
    name: "createCustomer",
    input: { source: "form.customer" }
  }
})
```

The browser sends an action invocation:

```json
{
  "action": "createCustomer",
  "input": {
    "name": "Acme",
    "level": "A"
  }
}
```

The host resolves the same published app version (or an explicitly supplied compatible version token), executes the named action in a fresh restricted context, validates the result and returns it.

Action results should use a small protocol such as:

```json
{
  "toast": { "type": "success", "message": "Created" },
  "refresh": ["customers"]
}
```

or return a complete new page definition when appropriate.

---

## 8. Execution lifecycle

Each request gets a fresh logical execution context.

```text
resolve app/version
 -> load immutable script artifact
 -> acquire/create restricted engine context
 -> inject SDK/context
 -> execute entry/action
 -> enforce timeout/call quotas
 -> serialize plain result
 -> schema validate result
 -> dispose/reset execution context
```

Do not allow scripts to create durable in-memory global state that affects other users.

Durable state must go through host capabilities.

---

## 9. Resource limits

The runtime must enforce configurable limits, including:

```text
maxExecutionMillis
maxCapabilityCalls
maxInputBytes
maxOutputBytes
maxLogEvents
maxPageNodes
maxTableRowsInDefinition
```

Suggested safe defaults can be introduced in configuration, but tests must verify the limit behavior.

A timeout/error must terminate the script request and return a structured error without destabilizing the JVM.

---

## 10. Engine sandbox requirements

For a GraalVM implementation, the intent is equivalent to:

- host access disabled
- host class lookup disabled
- native access disabled
- thread creation disabled
- process creation unavailable
- filesystem unavailable
- environment access unavailable
- unrestricted network APIs unavailable
- no `Java.type` or equivalent bridge

Exact API flags depend on the selected GraalVM version; implementation must include security tests proving these restrictions rather than relying only on configuration assumptions.

---

## 11. Validation

Validation has two levels.

### Static validation

Before publication:

- source parses
- exactly one app entry is defined
- exported actions have valid names
- manifest/app identifiers match
- source size within limit
- no unsupported syntax/module import pattern

### Runtime validation

During a validation execution:

- page entry completes within limits
- returned UI definition passes JSON schema validation
- all referenced named actions exist
- all referenced UI component types are supported
- capability calls use known capability names and valid schemas where determinable

Validation never grants extra permissions. A dedicated validation context may use mocked capability responses for structural validation.

---

## 12. Script SDK versioning

Scripts target an SDK version:

```json
{
  "sdkVersion": "1"
}
```

The host must keep SDK behavior compatible within a major version.

Do not silently change component semantics or capability invocation semantics for already published scripts.

---

## 13. Error model

Runtime errors returned to the browser/admin console should use stable codes:

```text
SCRIPT_PARSE_ERROR
SCRIPT_VALIDATION_ERROR
SCRIPT_TIMEOUT
SCRIPT_RUNTIME_ERROR
CAPABILITY_NOT_FOUND
CAPABILITY_INPUT_INVALID
CAPABILITY_PERMISSION_DENIED
CAPABILITY_POLICY_DENIED
CAPABILITY_EXECUTION_ERROR
UI_SCHEMA_INVALID
APP_DISABLED
APP_VERSION_NOT_FOUND
```

User-facing dynamic pages receive sanitized messages. Detailed stack traces belong only in authorized administration/audit diagnostics.

---

## 14. Example complete script

```javascript
export default defineApp({
  page: async () => {
    const result = await harness.call("crm.customer.search", {
      page: 1,
      size: 20
    });

    return page({
      title: "Customers",
      children: [
        button({
          text: "Refresh",
          action: { name: "refresh" }
        }),
        table({
          id: "customers",
          rows: result.items,
          columns: [
            { key: "name", label: "Name" },
            { key: "level", label: "Level" }
          ]
        })
      ]
    });
  },

  actions: {
    async refresh() {
      return {
        refresh: ["page"]
      };
    }
  }
});
```

The exact helper syntax may evolve before SDK v1 is frozen, but the architectural contract must remain: **restricted script -> typed host capabilities -> validated declarative UI**.
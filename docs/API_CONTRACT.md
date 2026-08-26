# RuoYi Harness — API and Data Contract

## 1. Scope

This document defines the stable HTTP/API boundary between the RuoYi host, Harness administration UI, and dynamic app renderer.

Exact URL prefixes may follow the upstream RuoYi conventions, but semantics must remain stable.

Recommended prefix:

```text
/harness
```

---

## 2. Administration API

### 2.1 Applications

```text
GET    /harness/apps
POST   /harness/apps
GET    /harness/apps/{appKey}
PUT    /harness/apps/{appKey}
POST   /harness/apps/{appKey}/enable
POST   /harness/apps/{appKey}/disable
```

Create request:

```json
{
  "appKey": "customer",
  "name": "Customer Management",
  "description": "Dynamic customer management application",
  "routeTitle": "Customers",
  "icon": "user"
}
```

### 2.2 Versions

```text
GET    /harness/apps/{appKey}/versions
POST   /harness/apps/{appKey}/versions
GET    /harness/apps/{appKey}/versions/{versionId}
PUT    /harness/apps/{appKey}/versions/{versionId}/source
POST   /harness/apps/{appKey}/versions/{versionId}/validate
POST   /harness/apps/{appKey}/versions/{versionId}/publish
POST   /harness/apps/{appKey}/rollback/{versionId}
DELETE /harness/apps/{appKey}/versions/{versionId}   # draft only
```

Published versions cannot be edited or deleted through normal operations.

Create version request:

```json
{
  "sdkVersion": "1",
  "source": "export default defineApp({...});"
}
```

Validation response:

```json
{
  "valid": true,
  "diagnostics": [],
  "sourceHash": "sha256:..."
}
```

Failure example:

```json
{
  "valid": false,
  "diagnostics": [
    {
      "severity": "error",
      "code": "SCRIPT_PARSE_ERROR",
      "message": "Unexpected token",
      "line": 12,
      "column": 8
    }
  ]
}
```

---

## 3. Runtime API

### 3.1 Resolve app descriptor

```text
GET /harness/runtime/apps/{appKey}
```

Response:

```json
{
  "appKey": "customer",
  "name": "Customer Management",
  "versionId": 42,
  "version": "7",
  "sdkVersion": "1",
  "enabled": true,
  "etag": "..."
}
```

The browser does not need raw script source for the default server-execution model.

### 3.2 Render page

```text
POST /harness/runtime/apps/{appKey}/render
```

Request:

```json
{
  "route": {},
  "state": {}
}
```

Response:

```json
{
  "appKey": "customer",
  "versionId": 42,
  "traceId": "...",
  "page": {
    "type": "page",
    "title": "Customers",
    "children": []
  }
}
```

### 3.3 Invoke action

```text
POST /harness/runtime/apps/{appKey}/actions/{actionName}
```

Request:

```json
{
  "versionId": 42,
  "input": {},
  "clientState": {}
}
```

The server must reject stale/incompatible action execution when a version-sensitive action cannot safely run against the currently published version. The implementation may either pin to the supplied immutable `versionId` for the duration of the open page or require a reload when publication changes; choose one explicit policy and test it.

Recommended policy: **pin the open page to its resolved immutable version until navigation/reload**. This prevents UI/action mismatch during a concurrent publish.

Response:

```json
{
  "traceId": "...",
  "effects": {
    "toast": {
      "type": "success",
      "message": "Saved"
    },
    "refresh": ["customers"]
  }
}
```

An action may alternatively return a replacement page definition:

```json
{
  "traceId": "...",
  "page": {
    "type": "page",
    "title": "Customers",
    "children": []
  }
}
```

---

## 4. Capability definition

Java-side conceptual model:

```java
public record CapabilityDefinition(
    String name,
    String version,
    String description,
    JsonSchema inputSchema,
    JsonSchema outputSchema,
    String requiredPermission,
    RiskLevel riskLevel,
    CapabilityHandler handler
) {}
```

Risk levels:

```text
READ
WRITE
SENSITIVE_WRITE
ADMIN
```

A capability registration collision on `(name, version)` must fail startup or explicit registration. Silent override is forbidden.

---

## 5. Capability invocation context

Every handler receives trusted host context, separate from script-controlled input:

```java
public record CapabilityContext(
    Long userId,
    String username,
    Set<String> permissions,
    String appKey,
    Long appVersionId,
    String requestId,
    String traceId
) {}
```

Scripts cannot construct or modify this context.

---

## 6. Capability Java API

Recommended registration style can be explicit Java configuration or an annotation adapter. The runtime depends on the registry abstraction, not annotations.

Example annotation convenience layer:

```java
@HarnessCapability(
    name = "crm.customer.search",
    permission = "crm:customer:list",
    risk = RiskLevel.READ
)
public CustomerPage search(CustomerSearchInput input, CapabilityContext ctx) {
    return customerService.search(input);
}
```

If annotation scanning is used, it must compile to the same `CapabilityDefinition` registry entries.

---

## 7. UI schema

A component always contains:

```json
{
  "type": "component-type",
  "id": "optional-stable-id"
}
```

### Page

```json
{
  "type": "page",
  "title": "Title",
  "children": []
}
```

### Table

```json
{
  "type": "table",
  "id": "customers",
  "columns": [
    {
      "key": "name",
      "label": "Name"
    }
  ],
  "rows": [
    {
      "name": "Acme"
    }
  ]
}
```

### Button

```json
{
  "type": "button",
  "text": "Save",
  "variant": "primary",
  "action": {
    "name": "save",
    "input": {}
  }
}
```

### Form

```json
{
  "type": "form",
  "id": "customerForm",
  "fields": [
    {
      "type": "input",
      "name": "name",
      "label": "Name",
      "required": true
    },
    {
      "type": "select",
      "name": "level",
      "label": "Level",
      "options": [
        { "label": "A", "value": "A" },
        { "label": "B", "value": "B" }
      ]
    }
  ]
}
```

The final schema should be represented as machine-readable JSON Schema files in the repository and used by both backend tests and frontend TypeScript generation/types.

---

## 8. Error envelope

All Harness APIs should follow the host project's standard response envelope where practical, while preserving a stable Harness error body.

Conceptual error:

```json
{
  "code": "CAPABILITY_PERMISSION_DENIED",
  "message": "Permission denied",
  "traceId": "...",
  "details": null
}
```

Never return Java stack traces to normal runtime users.

---

## 9. Database model

Use the host database. Recommended tables:

### `harness_app`

```text
id                  bigint PK
app_key             varchar unique
name                varchar
description         varchar/text
route_title         varchar
icon                 varchar
enabled              boolean
published_version_id bigint nullable
created_by           bigint
created_at           timestamp
updated_by           bigint
updated_at           timestamp
```

### `harness_app_version`

```text
id             bigint PK
app_id         bigint FK/version owner
version_no     bigint
sdk_version    varchar
source         text/longtext
source_hash    varchar
status         varchar
created_by     bigint
created_at     timestamp
validated_at   timestamp nullable
published_at   timestamp nullable
```

Unique constraint:

```text
(app_id, version_no)
```

### `harness_execution_log`

```text
id                 bigint PK
trace_id           varchar indexed
request_id         varchar
app_id             bigint
app_version_id     bigint
user_id            bigint
entry_type         varchar      # PAGE/ACTION
entry_name         varchar      # page or action name
status             varchar
started_at         timestamp
finished_at        timestamp
elapsed_ms         bigint
capability_calls   integer
error_code         varchar nullable
error_summary      varchar nullable
```

### `harness_capability_log`

```text
id                 bigint PK
trace_id           varchar indexed
execution_log_id   bigint
capability_name    varchar
capability_version varchar
user_id            bigint
risk_level         varchar
status             varchar
elapsed_ms         bigint
error_code         varchar nullable
created_at         timestamp
```

Do not persist secrets or full sensitive payloads by default. Payload auditing must use redaction rules.

---

## 10. Publication transaction

Publishing must be transactional:

```text
BEGIN
  verify version belongs to app
  verify version status == VALIDATED
  mark old published version SUPERSEDED where applicable
  mark target version PUBLISHED
  update harness_app.published_version_id
COMMIT
invalidate publication cache
```

If cache invalidation fails after commit, cache entries must have a bounded TTL and the system should attempt explicit retry/logging. Database publication state is authoritative.

Rollback uses the same pointer-switch transaction but targets a previously valid immutable version.

---

## 11. Menu integration

A dynamic app uses one stable host component/route and passes `appKey` as metadata or route parameter.

Conceptually:

```text
menu title: Customers
route: /harness/app/customer
component: HarnessDynamicAppView
permission: harness:app:customer:access
```

Creating/updating an app can synchronize a RuoYi menu record through the adapter layer. Avoid generating/compiling Vue source code for each dynamic app.

---

## 12. API compatibility rule

The contracts in this document are versioned independently from individual apps.

Breaking changes require a new Harness API/SDK major version. Published apps targeting an older supported SDK major version must continue to run or be explicitly migrated.
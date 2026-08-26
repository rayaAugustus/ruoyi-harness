package com.ruoyi.harness.runtime;

import com.ruoyi.harness.api.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class GraalHarnessScriptEngine implements HarnessScriptEngine {
    private static final Pattern FORBIDDEN_MODULE = Pattern.compile("(?m)^(\\s*)(import\\s|export\\s)|\\brequire\\s*\\(");
    private static final Pattern DEFINE_APP = Pattern.compile("\\bdefineApp\\s*\\(");
    private static final Pattern ACTION_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");
    private final ObjectMapper mapper;
    private final RuntimeLimits limits;
    private final SafeJsonBoundary json;
    private final UiDefinitionValidator ui;

    public GraalHarnessScriptEngine(ObjectMapper mapper, RuntimeLimits limits) {
        this.mapper = mapper; this.limits = limits;
        this.json = new SafeJsonBoundary(mapper, limits); this.ui = new UiDefinitionValidator(limits);
    }

    @Override
    public ValidationResult validate(ScriptArtifact artifact) {
        List<Diagnostic> diagnostics = staticDiagnostics(artifact);
        if (!diagnostics.isEmpty()) return new ValidationResult(false, diagnostics, hash(artifact.source()));
        try (Context context = newContext()) {
            install(context, artifact, null, new AtomicInteger(), new AtomicInteger());
            context.eval(Source.newBuilder("js", artifact.source(), artifact.appKey() + "-" + artifact.versionNo() + ".js").buildLiteral());
            Value app = context.getBindings("js").getMember("__harnessApp");
            if (app == null || app.isNull() || !app.hasMember("page") || !app.getMember("page").canExecute())
                diagnostics.add(Diagnostic.error(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "defineApp must register a page function"));
            if (app != null && app.hasMember("actions")) for (String name : app.getMember("actions").getMemberKeys())
                if (!ACTION_NAME.matcher(name).matches() || !app.getMember("actions").getMember(name).canExecute())
                    diagnostics.add(Diagnostic.error(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "Invalid action: " + name));
        } catch (PolyglotException e) { diagnostics.add(polyglotDiagnostic(e)); }
        catch (Exception e) { diagnostics.add(Diagnostic.error(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, e.getMessage())); }
        return new ValidationResult(diagnostics.isEmpty(), diagnostics, hash(artifact.source()));
    }

    @Override
    public ScriptExecutionResult execute(ScriptArtifact artifact, ScriptExecutionContext execution) {
        Objects.requireNonNull(execution); long started = System.nanoTime();
        List<Diagnostic> staticErrors = staticDiagnostics(artifact);
        if (!staticErrors.isEmpty()) return failed(staticErrors, HarnessErrorCode.SCRIPT_VALIDATION_ERROR, started, 0, 0);
        AtomicReference<Context> active = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger(); AtomicInteger logs = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "harness-script-execution"); thread.setDaemon(true); return thread;
        });
        Future<JsonNode> future = executor.submit(() -> executeInside(artifact, execution, active, calls, logs));
        try {
            JsonNode value = future.get(limits.maxExecutionMillis(), TimeUnit.MILLISECONDS);
            return new ScriptExecutionResult(true, value, List.of(), null, calls.get(), logs.get(), elapsed(started));
        } catch (TimeoutException e) {
            Context context = active.get(); if (context != null) try { context.close(true); } catch (Exception ignored) { }
            future.cancel(true);
            return failed(List.of(Diagnostic.error(HarnessErrorCode.SCRIPT_TIMEOUT, "Script execution timed out")),
                    HarnessErrorCode.SCRIPT_TIMEOUT, started, calls.get(), logs.get());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause(); HarnessErrorCode code = HarnessErrorCode.SCRIPT_RUNTIME_ERROR;
            if (cause instanceof HarnessException he) code = he.getCode();
            String message = cause == null ? "Script execution failed" : safeMessage(cause);
            return failed(List.of(Diagnostic.error(code, message)), code, started, calls.get(), logs.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failed(List.of(Diagnostic.error(HarnessErrorCode.SCRIPT_RUNTIME_ERROR, "Execution interrupted")),
                    HarnessErrorCode.SCRIPT_RUNTIME_ERROR, started, calls.get(), logs.get());
        } finally { executor.shutdownNow(); }
    }

    private JsonNode executeInside(ScriptArtifact artifact, ScriptExecutionContext execution,
            AtomicReference<Context> active, AtomicInteger calls, AtomicInteger logs) throws Exception {
        try (Context context = newContext()) {
            active.set(context); install(context, artifact, execution, calls, logs);
            context.eval(Source.newBuilder("js", artifact.source(), artifact.appKey() + "-" + artifact.versionNo() + ".js").buildLiteral());
            String selector = "PAGE".equalsIgnoreCase(execution.entryType())
                    ? "__harnessApp.page" : "(__harnessApp.actions && __harnessApp.actions[__entryName])";
            context.getBindings("js").putMember("__entryName", execution.entryName());
            context.eval("js", "globalThis.__done=false;globalThis.__failed=null;globalThis.__value=null;" +
                    "const __fn=" + selector + ";if(typeof __fn!=='function')throw new Error('ACTION_NOT_FOUND');" +
                    "Promise.resolve(__fn(harness.context,JSON.parse(__inputJson),JSON.parse(__stateJson)))" +
                    ".then(v=>{globalThis.__value=v;globalThis.__done=true},e=>{globalThis.__failed=e;globalThis.__done=true});");
            while (!context.getBindings("js").getMember("__done").asBoolean()) context.eval("js", "void 0");
            Value failed = context.getBindings("js").getMember("__failed");
            if (failed != null && !failed.isNull()) throw new HarnessException(mapGuestError(failed.toString()), "Script entry failed");
            JsonNode result = json.fromGuest(context.getBindings("js").getMember("__value"));
            if ("PAGE".equalsIgnoreCase(execution.entryType()) || "page".equals(result.path("type").asText())) ui.validatePage(result);
            return result;
        } catch (PolyglotException e) {
            if (e.isCancelled()) throw new HarnessException(HarnessErrorCode.SCRIPT_TIMEOUT, "Script execution timed out", e);
            if (e.isHostException() && e.asHostException() instanceof HarnessException he) throw he;
            HarnessErrorCode code = e.getMessage() != null && e.getMessage().contains("ACTION_NOT_FOUND")
                    ? HarnessErrorCode.ACTION_NOT_FOUND : HarnessErrorCode.SCRIPT_RUNTIME_ERROR;
            throw new HarnessException(code, "Script execution failed", e);
        } finally { active.set(null); }
    }

    private Context newContext() {
        return Context.newBuilder("js").allowHostAccess(HostAccess.NONE).allowHostClassLookup(name -> false)
                .allowNativeAccess(false).allowCreateThread(false).allowEnvironmentAccess(EnvironmentAccess.NONE)
                .allowIO(IOAccess.NONE).option("js.strict", "true").option("js.allow-eval", "false")
                .option("js.console", "false").option("engine.WarnInterpreterOnly", "false").build();
    }

    private void install(Context context, ScriptArtifact artifact, ScriptExecutionContext execution,
            AtomicInteger calls, AtomicInteger logs) throws Exception {
        Value bindings = context.getBindings("js");
        String input = execution == null ? "{}" : json.inputJson(execution.input());
        String state = execution == null ? "{}" : json.inputJson(execution.clientState());
        bindings.putMember("__inputJson", input); bindings.putMember("__stateJson", state);
        bindings.putMember("__hostCall", (ProxyExecutable) args -> {
            if (execution == null) throw new HarnessException(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "Capabilities are unavailable during static validation");
            int count = calls.incrementAndGet();
            if (count > limits.maxCapabilityCalls()) throw new HarnessException(HarnessErrorCode.CAPABILITY_CALL_LIMIT_EXCEEDED, "Capability call limit exceeded");
            String name = args[0].asString(); JsonNode request = mapper.readTree(args[1].asString());
            CapabilityContext ctx = new CapabilityContext(execution.identity().userId(), execution.identity().username(),
                    execution.identity().permissions(), artifact.appKey(), artifact.id(), execution.requestId(), execution.traceId());
            return mapper.writeValueAsString(execution.capabilityInvoker().invoke(name, request, ctx));
        });
        bindings.putMember("__hostLog", (ProxyExecutable) args -> {
            if (logs.incrementAndGet() > limits.maxLogEvents()) throw new HarnessException(HarnessErrorCode.LOG_LIMIT_EXCEEDED, "Log event limit exceeded");
            return null;
        });
        String contextJson = execution == null ? validationContext(artifact) : runtimeContext(artifact, execution);
        bindings.putMember("__contextJson", contextJson);
        context.eval("js", BOOTSTRAP);
    }

    private String runtimeContext(ScriptArtifact artifact, ScriptExecutionContext execution) throws Exception {
        return mapper.writeValueAsString(MapBuilder.context(artifact, execution));
    }
    private String validationContext(ScriptArtifact artifact) throws Exception { return mapper.writeValueAsString(MapBuilder.validation(artifact)); }

    private List<Diagnostic> staticDiagnostics(ScriptArtifact artifact) {
        List<Diagnostic> result = new ArrayList<>(); String source = artifact.source() == null ? "" : artifact.source();
        if (source.getBytes(StandardCharsets.UTF_8).length > limits.maxSourceBytes()) result.add(Diagnostic.error(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "Source exceeds size limit"));
        if (!"1".equals(artifact.sdkVersion())) result.add(Diagnostic.error(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "Unsupported SDK version"));
        if (FORBIDDEN_MODULE.matcher(source).find()) result.add(Diagnostic.error(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "Imports, exports and require are not supported in SDK v1"));
        Matcher matcher = DEFINE_APP.matcher(source); int count = 0; while (matcher.find()) count++;
        if (count != 1) result.add(Diagnostic.error(HarnessErrorCode.SCRIPT_VALIDATION_ERROR, "Script must call defineApp exactly once"));
        return result;
    }

    private static Diagnostic polyglotDiagnostic(PolyglotException e) {
        Integer line = null, column = null;
        if (e.getSourceLocation() != null) { line = e.getSourceLocation().getStartLine(); column = e.getSourceLocation().getStartColumn(); }
        return new Diagnostic("error", HarnessErrorCode.SCRIPT_PARSE_ERROR.name(), "JavaScript parse error", line, column);
    }
    private static String hash(String source) {
        try { return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static ScriptExecutionResult failed(List<Diagnostic> diagnostics, HarnessErrorCode code, long started, int calls, int logs) {
        return new ScriptExecutionResult(false, null, diagnostics, code.name(), calls, logs, elapsed(started));
    }
    private static long elapsed(long started) { return Duration.ofNanos(System.nanoTime() - started).toMillis(); }
    private static String safeMessage(Throwable error) { return error instanceof HarnessException ? error.getMessage() : "Script execution failed"; }
    private static HarnessErrorCode mapGuestError(String message) {
        if (message != null && message.contains("ACTION_NOT_FOUND")) return HarnessErrorCode.ACTION_NOT_FOUND;
        return HarnessErrorCode.SCRIPT_RUNTIME_ERROR;
    }

    private static final String BOOTSTRAP = """
        'use strict';
        globalThis.__harnessApp = null;
        const deepFreeze = value => { if (value && typeof value === 'object') { Object.freeze(value); Object.values(value).forEach(deepFreeze); } return value; };
        globalThis.defineApp = app => { if (globalThis.__harnessApp) throw new Error('defineApp called more than once'); globalThis.__harnessApp = app; return app; };
        const component = type => value => Object.assign({type}, value || {});
        globalThis.page=component('page');globalThis.section=component('section');globalThis.text=component('text');
        globalThis.statistic=component('statistic');globalThis.table=component('table');globalThis.form=component('form');
        globalThis.input=component('input');globalThis.select=component('select');globalThis.button=component('button');
        globalThis.tabs=component('tabs');globalThis.modal=component('modal');globalThis.alert=component('alert');globalThis.chart=component('chart');
        const ctx=deepFreeze(JSON.parse(__contextJson));
        globalThis.harness=deepFreeze({context:ctx,call:(name,input)=>JSON.parse(__hostCall(String(name),JSON.stringify(input||{}))),
          log:{debug:(...a)=>__hostLog('debug'),info:(...a)=>__hostLog('info'),warn:(...a)=>__hostLog('warn'),error:(...a)=>__hostLog('error')}});
        """;

    private static final class MapBuilder {
        static java.util.Map<String,Object> validation(ScriptArtifact a) { return java.util.Map.of("app", java.util.Map.of("key", a.appKey(), "version", String.valueOf(a.versionNo())), "user", java.util.Map.of("id", "validation", "name", "validation"), "requestId", "validation", "traceId", "validation"); }
        static java.util.Map<String,Object> context(ScriptArtifact a, ScriptExecutionContext c) { return java.util.Map.of("app", java.util.Map.of("key", a.appKey(), "version", String.valueOf(a.versionNo())), "user", java.util.Map.of("id", String.valueOf(c.identity().userId()), "name", c.identity().username()), "locale", c.locale() == null ? "" : c.locale(), "requestId", c.requestId(), "traceId", c.traceId()); }
    }
}

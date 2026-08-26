package com.ruoyi.harness.runtime;

import com.ruoyi.harness.api.HarnessErrorCode;
import com.ruoyi.harness.api.HarnessException;
import com.ruoyi.harness.api.RuntimeLimits;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class SafeJsonBoundary {
    private final ObjectMapper mapper;
    private final RuntimeLimits limits;

    public SafeJsonBoundary(ObjectMapper mapper, RuntimeLimits limits) { this.mapper = mapper; this.limits = limits; }

    public JsonNode fromGuest(Value value) {
        JsonNode node = convert(value, 0);
        try {
            int bytes = mapper.writeValueAsBytes(node).length;
            if (bytes > limits.maxOutputBytes()) throw new HarnessException(HarnessErrorCode.OUTPUT_LIMIT_EXCEEDED,
                    "Script output exceeds " + limits.maxOutputBytes() + " bytes");
        } catch (HarnessException e) { throw e; }
        catch (Exception e) { throw new HarnessException(HarnessErrorCode.SCRIPT_RUNTIME_ERROR, "Cannot serialize script output", e); }
        return node;
    }

    public String inputJson(JsonNode node) {
        JsonNode safe = node == null ? mapper.createObjectNode() : node;
        try {
            byte[] bytes = mapper.writeValueAsBytes(safe);
            if (bytes.length > limits.maxInputBytes()) throw new HarnessException(HarnessErrorCode.INPUT_LIMIT_EXCEEDED,
                    "Script input exceeds " + limits.maxInputBytes() + " bytes");
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (HarnessException e) { throw e; }
        catch (Exception e) { throw new HarnessException(HarnessErrorCode.SCRIPT_RUNTIME_ERROR, "Cannot serialize script input", e); }
    }

    private JsonNode convert(Value value, int depth) {
        if (depth > limits.maxJsonDepth()) throw new HarnessException(HarnessErrorCode.OUTPUT_LIMIT_EXCEEDED, "JSON depth exceeded");
        if (value == null || value.isNull()) return mapper.nullNode();
        if (value.isBoolean()) return mapper.getNodeFactory().booleanNode(value.asBoolean());
        if (value.isString()) return mapper.getNodeFactory().textNode(value.asString());
        if (value.isNumber()) {
            if (value.fitsInLong()) return mapper.getNodeFactory().numberNode(value.asLong());
            if (value.fitsInDouble()) return mapper.getNodeFactory().numberNode(value.asDouble());
            throw new HarnessException(HarnessErrorCode.SCRIPT_RUNTIME_ERROR, "Unsupported numeric value");
        }
        if (value.hasArrayElements()) {
            long size = value.getArraySize();
            if (size > limits.maxPageNodes() * 4L) throw new HarnessException(HarnessErrorCode.OUTPUT_LIMIT_EXCEEDED, "Array is too large");
            ArrayNode array = mapper.createArrayNode();
            for (long i = 0; i < size; i++) array.add(convert(value.getArrayElement(i), depth + 1));
            return array;
        }
        if (value.hasMembers() && !value.isHostObject()) {
            ObjectNode object = mapper.createObjectNode();
            for (String key : value.getMemberKeys()) {
                Value member = value.getMember(key);
                if (member != null && member.canExecute()) throw new HarnessException(HarnessErrorCode.SCRIPT_RUNTIME_ERROR,
                        "Functions cannot cross the runtime boundary");
                object.set(key, convert(member, depth + 1));
            }
            return object;
        }
        throw new HarnessException(HarnessErrorCode.SCRIPT_RUNTIME_ERROR, "Non-JSON value returned by script");
    }
}

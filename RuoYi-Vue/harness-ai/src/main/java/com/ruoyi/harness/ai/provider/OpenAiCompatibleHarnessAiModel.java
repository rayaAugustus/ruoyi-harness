package com.ruoyi.harness.ai.provider;

import com.ruoyi.harness.ai.model.*;
import com.ruoyi.harness.ai.port.HarnessAiModel;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class OpenAiCompatibleHarnessAiModel implements HarnessAiModel {
    private final ObjectMapper mapper; private final StructuredAiResponseParser parser; private final HttpClient client;
    private final String baseUrl, apiKey, model, systemPrompt; private final Duration readTimeout;
    private final boolean enabled;

    public OpenAiCompatibleHarnessAiModel(ObjectMapper mapper, boolean enabled, String baseUrl, String apiKey,
            String model, Duration connectTimeout, Duration readTimeout, String systemPrompt) {
        this.mapper=mapper;this.parser=new StructuredAiResponseParser(mapper);this.enabled=enabled;
        this.baseUrl=trimSlash(baseUrl);this.apiKey=apiKey;this.model=model;this.readTimeout=readTimeout;this.systemPrompt=systemPrompt;
        this.client=HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    @Override public AiGenerationResult generate(AiGenerationRequest request) {
        if (!enabled) throw new HarnessAiException(AiErrorCode.AI_DISABLED, "AI Builder is disabled");
        if (blank(baseUrl)||blank(apiKey)||blank(model)) throw new HarnessAiException(AiErrorCode.AI_PROVIDER_UNAVAILABLE, "AI provider is not fully configured");
        try {
            ObjectNode body=mapper.createObjectNode();body.put("model",model);body.put("temperature",request.options().temperature());body.put("max_tokens",request.options().maxOutputTokens());
            ObjectNode responseFormat=mapper.createObjectNode();responseFormat.put("type","json_object");body.set("response_format",responseFormat);
            ArrayNode messages=mapper.createArrayNode();messages.add(message("system",systemPrompt));messages.add(message("system",mapper.writeValueAsString(request.context())));
            for(AiMessage item:request.messages())messages.add(message(item.role(),item.content()));body.set("messages",messages);
            HttpRequest http=HttpRequest.newBuilder(URI.create(baseUrl+"/chat/completions")).timeout(readTimeout)
                    .header("Authorization","Bearer "+apiKey).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response=client.send(http,HttpResponse.BodyHandlers.ofString());mapStatus(response.statusCode());
            JsonNode root=mapper.readTree(response.body());JsonNode choice=root.path("choices").path(0).path("message").path("content");
            if(!choice.isTextual())throw new HarnessAiException(AiErrorCode.AI_RESPONSE_INVALID,"AI response did not contain structured content");
            Usage usage=new Usage(longOrNull(root.path("usage").get("prompt_tokens")),longOrNull(root.path("usage").get("completion_tokens")));
            return parser.parse(choice.asText(),root.path("model").asText(model),"openai-compatible",usage);
        } catch (HarnessAiException e) { throw e; }
        catch (HttpTimeoutException e) { throw new HarnessAiException(AiErrorCode.AI_TIMEOUT,"AI provider timed out",e); }
        catch (InterruptedException e) { Thread.currentThread().interrupt();throw new HarnessAiException(AiErrorCode.AI_PROVIDER_UNAVAILABLE,"AI request interrupted",e); }
        catch (IOException|IllegalArgumentException e) { throw new HarnessAiException(AiErrorCode.AI_PROVIDER_UNAVAILABLE,"AI provider unavailable",e); }
        catch (Exception e) { throw new HarnessAiException(AiErrorCode.AI_RESPONSE_INVALID,"Unable to process AI response",e); }
    }
    private ObjectNode message(String role,String content){ObjectNode value=mapper.createObjectNode();value.put("role",role);value.put("content",content);return value;}
    private static void mapStatus(int status){if(status>=200&&status<300)return;if(status==401||status==403)throw new HarnessAiException(AiErrorCode.AI_PROVIDER_AUTH_FAILED,"AI provider rejected credentials");if(status==429)throw new HarnessAiException(AiErrorCode.AI_RATE_LIMITED,"AI provider rate limit exceeded");if(status==408||status==504)throw new HarnessAiException(AiErrorCode.AI_TIMEOUT,"AI provider timed out");throw new HarnessAiException(AiErrorCode.AI_PROVIDER_UNAVAILABLE,"AI provider returned HTTP "+status);}
    private static Long longOrNull(JsonNode node){return node==null||!node.isNumber()?null:node.asLong();}
    private static String trimSlash(String value){if(value==null)return null;return value.endsWith("/")?value.substring(0,value.length()-1):value;}
    private static boolean blank(String value){return value==null||value.isBlank();}
}

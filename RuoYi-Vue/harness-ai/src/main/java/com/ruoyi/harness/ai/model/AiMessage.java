package com.ruoyi.harness.ai.model;

public record AiMessage(String role, String content) {
    public AiMessage {
        role = role == null ? "user" : role.toLowerCase();
        content = content == null ? "" : content;
    }
}

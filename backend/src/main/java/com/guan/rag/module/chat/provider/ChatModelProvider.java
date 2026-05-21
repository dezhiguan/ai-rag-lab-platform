package com.guan.rag.module.chat.provider;

public interface ChatModelProvider {

    ChatResult chat(String prompt);

    String model();

    record ChatResult(String answer, int promptTokens, int completionTokens, long latencyMs) {
    }
}

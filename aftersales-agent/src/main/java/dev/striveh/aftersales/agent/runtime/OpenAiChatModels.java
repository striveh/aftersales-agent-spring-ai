package dev.striveh.aftersales.agent.runtime;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public final class OpenAiChatModels {

    private OpenAiChatModels() {
    }

    public static ChatModel create(String baseUrl, String apiKey, String model, double temperature) {
        String url = baseUrl == null || baseUrl.isBlank() ? "https://api.deepseek.com" : strip(baseUrl);
        String name = model == null || model.isBlank() ? "deepseek-chat" : model;
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder().baseUrl(url).apiKey(apiKey).build())
                .defaultOptions(OpenAiChatOptions.builder().model(name).temperature(temperature).build())
                .build();
    }

    private static String strip(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

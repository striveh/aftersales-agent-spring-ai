package dev.striveh.aftersales.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String clock = "system";
    private final Llm llm = new Llm();
    private final Retrieval retrieval = new Retrieval();
    private final Tools tools = new Tools();
    private final Plan plan = new Plan();

    public String getClock() {
        return clock;
    }

    public void setClock(String clock) {
        this.clock = clock;
    }

    public Llm getLlm() {
        return llm;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public Tools getTools() {
        return tools;
    }

    public Plan getPlan() {
        return plan;
    }

    public String resolvedProvider() {
        String provider = llm.getProvider() == null ? "auto" : llm.getProvider().trim();
        if ("stub".equalsIgnoreCase(provider)) {
            return "stub";
        }
        if ("openai".equalsIgnoreCase(provider) && hasKey()) {
            return "openai";
        }
        if ("auto".equalsIgnoreCase(provider) && hasKey()) {
            return "openai";
        }
        return "stub";
    }

    private boolean hasKey() {
        String key = llm.getApiKey();
        return key != null && !key.isBlank() && !key.startsWith("sk-placeholder");
    }

    public static class Llm {
        private String provider = "auto";
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private double temperature = 0.2;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }

    public static class Retrieval {
        private String backend = "memory";
        private int topK = 4;
        private int candidateK = 8;

        public String getBackend() {
            return backend;
        }

        public void setBackend(String backend) {
            this.backend = backend;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public int getCandidateK() {
            return candidateK;
        }

        public void setCandidateK(int candidateK) {
            this.candidateK = candidateK;
        }
    }

    public static class Tools {
        private String mode = "local";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class Plan {
        private boolean allowFallback = true;

        public boolean isAllowFallback() {
            return allowFallback;
        }

        public void setAllowFallback(boolean allowFallback) {
            this.allowFallback = allowFallback;
        }
    }
}

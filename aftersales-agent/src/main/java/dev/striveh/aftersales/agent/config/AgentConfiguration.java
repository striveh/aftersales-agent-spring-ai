package dev.striveh.aftersales.agent.config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.striveh.aftersales.agent.plan.HeuristicPlans;
import dev.striveh.aftersales.agent.plan.HeuristicChatModel;
import dev.striveh.aftersales.agent.retrieval.HashingEmbeddingModel;
import dev.striveh.aftersales.agent.retrieval.InMemoryPolicyIndex;
import dev.striveh.aftersales.agent.retrieval.PgPolicyIndex;
import dev.striveh.aftersales.agent.retrieval.PolicyIndex;
import dev.striveh.aftersales.agent.runtime.AgentRuntime;
import dev.striveh.aftersales.agent.runtime.OpenAiChatModels;
import dev.striveh.aftersales.agent.tool.LocalOrderGateway;
import dev.striveh.aftersales.agent.tool.McpOrderGateway;
import dev.striveh.aftersales.agent.tool.OrderGateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AgentConfiguration.class);

    @Bean
    Clock clock(AgentProperties properties) {
        if (properties.getClock() == null || properties.getClock().isBlank() || "system".equals(properties.getClock())) {
            return Clock.system(AgentRuntime.ZONE);
        }
        return Clock.fixed(Instant.parse(properties.getClock()), AgentRuntime.ZONE);
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new HashingEmbeddingModel();
    }

    @Bean
    @ConditionalOnProperty(name = "agent.retrieval.backend", havingValue = "memory", matchIfMissing = true)
    PolicyIndex memoryPolicyIndex(EmbeddingModel embeddingModel) {
        return new InMemoryPolicyIndex((HashingEmbeddingModel) embeddingModel);
    }

    @Bean
    @ConditionalOnProperty(name = "agent.retrieval.backend", havingValue = "pgvector")
    PolicyIndex pgPolicyIndex(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        return new PgPolicyIndex(vectorStore, jdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "agent.tools.mode", havingValue = "local", matchIfMissing = true)
    OrderGateway localOrderGateway(Clock clock) {
        return new LocalOrderGateway(LocalDate.now(clock));
    }

    @Bean
    @ConditionalOnProperty(name = "agent.tools.mode", havingValue = "mcp")
    OrderGateway mcpOrderGateway(ToolCallbackProvider tools, ObjectMapper objectMapper) {
        return new McpOrderGateway(tools, objectMapper);
    }

    @Bean
    ChatModel chatModel(AgentProperties properties, ObjectMapper objectMapper) {
        if ("openai".equals(properties.resolvedProvider())) {
            log.info("llm provider=openai model={} baseUrl={}", properties.getLlm().getModel(), properties.getLlm().getBaseUrl());
            return OpenAiChatModels.create(properties.getLlm().getBaseUrl(), properties.getLlm().getApiKey(),
                    properties.getLlm().getModel(), properties.getLlm().getTemperature());
        }
        log.info("llm provider=stub（未配置可用的 API key，规划走确定性规则）");
        return new HeuristicChatModel(new HeuristicPlans(), objectMapper);
    }

    @Bean
    AgentRuntime agentRuntime(Clock clock, AgentProperties properties, PolicyIndex policyIndex, OrderGateway orderGateway,
                              ChatModel chatModel, ObjectMapper objectMapper) {
        boolean heuristic = !"openai".equals(properties.resolvedProvider());
        log.info("retrieval backend={} tools={} planFallback={}", properties.getRetrieval().getBackend(),
                properties.getTools().getMode(), properties.getPlan().isAllowFallback());
        return AgentRuntime.create(clock, policyIndex, orderGateway, chatModel, new HeuristicPlans(),
                        properties.getPlan().isAllowFallback(), properties.getRetrieval().getCandidateK(),
                        properties.getRetrieval().getTopK(), heuristic ? "stub" : "openai", heuristic, objectMapper)
                .ingestClasspath();
    }
}

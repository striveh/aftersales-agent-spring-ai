package dev.striveh.aftersales.agent.runtime;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.striveh.aftersales.agent.answer.AnswerComposer;
import dev.striveh.aftersales.agent.answer.AnswerPolisher;
import dev.striveh.aftersales.agent.eligibility.EligibilityDecider;
import dev.striveh.aftersales.agent.plan.ChatPlanner;
import dev.striveh.aftersales.agent.plan.HeuristicChatModel;
import dev.striveh.aftersales.agent.plan.HeuristicPlans;
import dev.striveh.aftersales.agent.plan.PlanAndExecuteAgent;
import dev.striveh.aftersales.agent.plan.PlanParser;
import dev.striveh.aftersales.agent.plan.StepExecutor;
import dev.striveh.aftersales.agent.retrieval.HashingEmbeddingModel;
import dev.striveh.aftersales.agent.retrieval.HybridPolicyRetriever;
import dev.striveh.aftersales.agent.retrieval.InMemoryPolicyIndex;
import dev.striveh.aftersales.agent.retrieval.MarkdownPolicyChunker;
import dev.striveh.aftersales.agent.retrieval.PolicyCorpus;
import dev.striveh.aftersales.agent.retrieval.PolicyIndex;
import dev.striveh.aftersales.agent.retrieval.PolicySource;
import dev.striveh.aftersales.agent.tool.ApprovalService;
import dev.striveh.aftersales.agent.tool.LocalOrderGateway;
import dev.striveh.aftersales.agent.tool.OrderGateway;
import dev.striveh.aftersales.agent.tool.RefundLedger;
import dev.striveh.aftersales.agent.tool.RefundTool;
import dev.striveh.aftersales.agent.trace.TraceStore;

import org.springframework.ai.chat.model.ChatModel;

public final class AgentRuntime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final PlanAndExecuteAgent agent;
    private final ApprovalService approvals;
    private final RefundLedger ledger;
    private final TraceStore traces;
    private final PolicyIndex index;
    private final String mode;
    private final boolean heuristic;
    private final LocalDate today;

    private AgentRuntime(PlanAndExecuteAgent agent, ApprovalService approvals, RefundLedger ledger, TraceStore traces,
                         PolicyIndex index, String mode, boolean heuristic, LocalDate today) {
        this.agent = agent;
        this.approvals = approvals;
        this.ledger = ledger;
        this.traces = traces;
        this.index = index;
        this.mode = mode;
        this.heuristic = heuristic;
        this.today = today;
    }

    public static AgentRuntime offline(LocalDate today) {
        Clock clock = Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
        ObjectMapper json = JsonSupport.mapper();
        HeuristicPlans heuristics = new HeuristicPlans();
        return create(clock, new InMemoryPolicyIndex(new HashingEmbeddingModel()), new LocalOrderGateway(today),
                new HeuristicChatModel(heuristics, json), heuristics, true, 8, 4, "stub", true, json);
    }

    public static AgentRuntime create(Clock clock, PolicyIndex index, OrderGateway gateway, ChatModel model,
                                      HeuristicPlans heuristics, boolean allowFallback, int candidateK, int topK,
                                      String mode, boolean heuristic, ObjectMapper json) {
        LocalDate today = LocalDate.now(clock);
        RefundLedger ledger = new RefundLedger(clock);
        ApprovalService approvals = new ApprovalService(clock, ledger);
        RefundTool refundTool = new RefundTool(approvals);
        HybridPolicyRetriever retriever = new HybridPolicyRetriever(index, candidateK, topK);
        AnswerComposer composer = new AnswerComposer(index);
        EligibilityDecider decider = new EligibilityDecider();
        StepExecutor executor = new StepExecutor(gateway, retriever, decider, composer, refundTool, approvals, json, today);
        ChatPlanner planner = new ChatPlanner(model, new PlanParser(json), heuristics, allowFallback);
        TraceStore traces = new TraceStore(clock);
        AnswerPolisher polisher = heuristic ? null : new AnswerPolisher(model);
        PlanAndExecuteAgent agent = new PlanAndExecuteAgent(planner, executor, composer, traces, today, mode, polisher);
        return new AgentRuntime(agent, approvals, ledger, traces, index, mode, heuristic, today);
    }

    public AgentRuntime ingestClasspath() {
        return ingest(new PolicyCorpus().load());
    }

    public AgentRuntime ingest(List<PolicySource> sources) {
        index.replaceAll(new MarkdownPolicyChunker().chunkAll(sources));
        return this;
    }

    public PlanAndExecuteAgent agent() {
        return agent;
    }

    public ApprovalService approvals() {
        return approvals;
    }

    public RefundLedger ledger() {
        return ledger;
    }

    public TraceStore traces() {
        return traces;
    }

    public PolicyIndex index() {
        return index;
    }

    public String mode() {
        return mode;
    }

    public boolean heuristic() {
        return heuristic;
    }

    public LocalDate today() {
        return today;
    }
}

package dev.striveh.aftersales.agent.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import dev.striveh.aftersales.agent.answer.AnswerComposer;
import dev.striveh.aftersales.agent.answer.AnswerPolisher;
import dev.striveh.aftersales.agent.eligibility.DecisionCode;
import dev.striveh.aftersales.agent.eligibility.EligibilityDecision;
import dev.striveh.aftersales.agent.eligibility.UserIntent;
import dev.striveh.aftersales.agent.trace.TraceEvent;
import dev.striveh.aftersales.agent.trace.TraceStore;

public final class PlanAndExecuteAgent {

    private static final int MAX_REPLANS = 2;

    private final ChatPlanner planner;
    private final StepExecutor executor;
    private final AnswerComposer composer;
    private final TraceStore traces;
    private final java.time.LocalDate today;
    private final String mode;
    private final AnswerPolisher polisher;
    private final ConcurrentMap<String, ExecutionState> memory = new ConcurrentHashMap<>();

    public PlanAndExecuteAgent(ChatPlanner planner, StepExecutor executor, AnswerComposer composer, TraceStore traces,
                               java.time.LocalDate today, String mode, AnswerPolisher polisher) {
        this.planner = planner;
        this.executor = executor;
        this.composer = composer;
        this.traces = traces;
        this.today = today;
        this.mode = mode;
        this.polisher = polisher;
    }

    public AgentTurn handle(String conversationId, String customerPhone, String message) {
        String id = conversationId == null || conversationId.isBlank() ? UUID.randomUUID().toString() : conversationId;
        ExecutionState state = memory.computeIfAbsent(id, key -> new ExecutionState(key, customerPhone, message));
        state.customerPhone(customerPhone);
        state.message(message);
        state.intent(UserIntent.classify(message));
        state.pending(null);
        state.answer("");
        state.lookupFailed(false);
        state.clearTurnActions();
        int traceStart = traces.get(id).size();

        PlanContext context = context(state);
        Planned planned = planner.plan(context);
        boolean fellBack = planned.usedFallback();
        Plan plan = planned.plan();
        traces.add(id, "PLAN", "", fellBack ? "FALLBACK" : "OK", describe(plan));

        int replans = 0;
        boolean replanned = false;
        while (true) {
            StepResult failure = null;
            PlanStep failedStep = null;
            for (PlanStep step : plan.steps()) {
                StepResult result = executor.execute(step, state);
                traces.add(id, "STEP", step.action(), result.status().name(), step.purpose() + " " + result.detail());
                if (result.status() == StepStatus.FAILED) {
                    failure = result;
                    failedStep = step;
                    break;
                }
                if (result.status() == StepStatus.AWAITING_APPROVAL) {
                    ensureAnswer(state);
                    memory.put(id, state);
                    return turn(state, traceStart, replanned, fellBack);
                }
            }
            if (failure == null) {
                ensureAnswer(state);
                memory.put(id, state);
                return turn(state, traceStart, replanned, fellBack);
            }
            PlanContext replanContext = context(state);
            Planned next = planner.replan(replanContext, failedStep.action(), failure.detail());
            fellBack = fellBack || next.usedFallback();
            if (replans >= MAX_REPLANS || next.plan().sameSteps(plan)) {
                DecisionCode code = failure.detail().contains("需要订单号")
                        ? DecisionCode.NEED_CLARIFICATION
                        : DecisionCode.LOOKUP_FAILED;
                state.decision(new EligibilityDecision(code, false, null, null, failure.detail(), state.order()));
                state.answer(composer.compose(state, today));
                traces.add(id, "REPLAN_STOPPED", failedStep.action(), "FAILED", failure.detail());
                memory.put(id, state);
                return turn(state, traceStart, replanned, fellBack);
            }
            replans++;
            replanned = true;
            plan = next.plan();
            traces.add(id, "REPLAN", failedStep.action(), "OK", describe(plan));
            state.clearTurnActions();
        }
    }

    private void ensureAnswer(ExecutionState state) {
        if (state.answer() == null || state.answer().isBlank()) {
            if (state.decision() == null) {
                executor.execute(new PlanStep("draft", AgentAction.DRAFT_ANSWER.wire(), java.util.Map.of(), "补写答复"), state);
            } else {
                state.answer(composer.compose(state, today));
            }
        }
        if (polisher != null && state.decision() != null && state.answer() != null && !state.answer().isBlank()) {
            state.answer(polisher.polish(state.message(), state.answer(), state.decision().code().name()));
        }
    }

    private AgentTurn turn(ExecutionState state, int traceStart, boolean replanned, boolean fellBack) {
        List<TraceEvent> all = traces.get(state.conversationId());
        List<TraceEvent> fresh = new ArrayList<>(all.subList(Math.min(traceStart, all.size()), all.size()));
        return new AgentTurn(
                state.conversationId(),
                state.answer(),
                state.decision() == null ? DecisionCode.LOOKUP_FAILED : state.decision().code(),
                state.decision() != null && state.decision().refundable(),
                state.order() == null ? "" : state.order().orderId(),
                state.citations(),
                state.pending(),
                fresh,
                replanned,
                fellBack,
                mode);
    }

    private PlanContext context(ExecutionState state) {
        boolean refundable = state.decision() != null && state.decision().refundable();
        String known = state.order() == null ? "" : state.order().orderId();
        return new PlanContext(state.message(), state.customerPhone(), known, refundable, false);
    }

    private String describe(Plan plan) {
        return plan.reason() + " -> " + plan.steps().stream().map(PlanStep::action).reduce((left, right) -> left + "," + right).orElse("");
    }

    public ExecutionState memory(String conversationId) {
        return memory.get(conversationId);
    }
}

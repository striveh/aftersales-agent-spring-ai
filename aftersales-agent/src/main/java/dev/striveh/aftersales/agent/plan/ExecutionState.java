package dev.striveh.aftersales.agent.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.striveh.aftersales.agent.answer.Citation;
import dev.striveh.aftersales.agent.eligibility.EligibilityDecision;
import dev.striveh.aftersales.agent.eligibility.Intent;
import dev.striveh.aftersales.agent.retrieval.PolicyChunk;
import dev.striveh.aftersales.agent.tool.Approval;
import dev.striveh.aftersales.common.Logistics;
import dev.striveh.aftersales.common.Order;

/** Blackboard for one conversation. Follow-up turns reuse the verified order. */
public final class ExecutionState {

    private final String conversationId;
    private String customerPhone;
    private String message;
    private Intent intent = Intent.RETURN;
    private Order order;
    private List<Order> candidates = List.of();
    private Logistics logistics;
    private List<PolicyChunk> clauses = List.of();
    private List<Citation> citations = List.of();
    private EligibilityDecision decision;
    private String answer = "";
    private Approval pending;
    private boolean lookupFailed;
    private final Map<String, Map<String, String>> outputs = new LinkedHashMap<>();
    private final List<String> actions = new ArrayList<>();

    public ExecutionState(String conversationId, String customerPhone, String message) {
        this.conversationId = conversationId;
        this.customerPhone = customerPhone == null ? "" : customerPhone;
        this.message = message == null ? "" : message;
    }

    public String conversationId() {
        return conversationId;
    }

    public String customerPhone() {
        return customerPhone;
    }

    public void customerPhone(String customerPhone) {
        if (customerPhone != null && !customerPhone.isBlank()) {
            this.customerPhone = customerPhone;
        }
    }

    public String message() {
        return message;
    }

    public void message(String message) {
        this.message = message == null ? "" : message;
    }

    public Intent intent() {
        return intent;
    }

    public void intent(Intent intent) {
        this.intent = intent;
    }

    public Order order() {
        return order;
    }

    public void order(Order order) {
        this.order = order;
    }

    public List<Order> candidates() {
        return candidates;
    }

    public void candidates(List<Order> candidates) {
        this.candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public Logistics logistics() {
        return logistics;
    }

    public void logistics(Logistics logistics) {
        this.logistics = logistics;
    }

    public List<PolicyChunk> clauses() {
        return clauses;
    }

    public void clauses(List<PolicyChunk> clauses) {
        this.clauses = clauses == null ? List.of() : List.copyOf(clauses);
    }

    public List<Citation> citations() {
        return citations;
    }

    public void citations(List<Citation> citations) {
        this.citations = citations == null ? List.of() : List.copyOf(citations);
    }

    public EligibilityDecision decision() {
        return decision;
    }

    public void decision(EligibilityDecision decision) {
        this.decision = decision;
    }

    public String answer() {
        return answer;
    }

    public void answer(String answer) {
        this.answer = answer == null ? "" : answer;
    }

    public Approval pending() {
        return pending;
    }

    public void pending(Approval pending) {
        this.pending = pending;
    }

    public boolean lookupFailed() {
        return lookupFailed;
    }

    public void lookupFailed(boolean lookupFailed) {
        this.lookupFailed = lookupFailed;
    }

    public void remember(String stepId, Map<String, String> output) {
        outputs.put(stepId, Map.copyOf(output));
    }

    public Map<String, String> output(String stepId) {
        return outputs.getOrDefault(stepId, Map.of());
    }

    public void action(String action) {
        actions.add(action);
    }

    public List<String> actions() {
        return List.copyOf(actions);
    }

    public void clearTurnActions() {
        actions.clear();
    }
}

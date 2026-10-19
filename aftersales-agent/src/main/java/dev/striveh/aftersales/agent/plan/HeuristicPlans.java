package dev.striveh.aftersales.agent.plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.striveh.aftersales.agent.eligibility.Intent;
import dev.striveh.aftersales.agent.eligibility.UserIntent;

/**
 * Deterministic planner used by the offline chat model. A live model receives the
 * same task prompt and is expected to return the same JSON shape.
 */
public final class HeuristicPlans {

    private static final Pattern ORDER_ID = Pattern.compile("ORD-[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*");

    public Plan plan(PlanContext context) {
        Intent intent = UserIntent.classify(context.message());
        if (intent == Intent.REFUND && context.previousRefundable() && !context.knownOrderId().isBlank()
                && !context.message().contains("订单")) {
            return new Plan("上一轮已经确认可退，这一轮只提交退款申请", List.of(
                    step("s1", AgentAction.REQUEST_REFUND, Map.of(), "用户明确要求退款，生成待确认申请"),
                    step("s2", AgentAction.DRAFT_ANSWER, Map.of(), "说明退款尚未打款")
            ));
        }
        return full(context, intent);
    }

    public Plan replan(PlanContext context, String failedAction, String failureMessage) {
        if ("lookup_order".equals(failedAction) && failureMessage != null && failureMessage.contains("没有找到订单")) {
            return full(context.withoutOrderId(), UserIntent.classify(context.message()));
        }
        return full(context.withoutOrderId(), UserIntent.classify(context.message()));
    }

    private Plan full(PlanContext context, Intent intent) {
        if (intent == Intent.SMALL_TALK || intent == Intent.OUT_OF_SCOPE) {
            return new Plan("不需要查订单", List.of(step("s1", AgentAction.DRAFT_ANSWER, Map.of(), "直接答复")));
        }
        List<PlanStep> steps = new ArrayList<>();
        if (intent != Intent.POLICY) {
            steps.add(step("s1", AgentAction.LOOKUP_ORDER, orderArgs(context), "定位订单"));
            steps.add(step("s2", AgentAction.LOOKUP_LOGISTICS, Map.of("orderId", "$s1.orderId"), "读取签收和轨迹"));
        }
        if (intent != Intent.LOGISTICS) {
            steps.add(step(nextId(steps), AgentAction.RETRIEVE_POLICY, Map.of("query", policyQuery(context, intent)),
                    "检索要引用的制度条款"));
        }
        steps.add(step(nextId(steps), AgentAction.DECIDE_ELIGIBILITY, Map.of(), "按条款做资格判定"));
        if (intent == Intent.REFUND) {
            steps.add(step(nextId(steps), AgentAction.REQUEST_REFUND, Map.of(), "资格允许时才生成待确认退款"));
        }
        steps.add(step(nextId(steps), AgentAction.DRAFT_ANSWER, Map.of(), "组织带引用的答复"));
        return new Plan(intent.name(), steps);
    }

    private Map<String, Object> orderArgs(PlanContext context) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("orderId", context.ignoreOrderIdInMessage() ? "" : extractOrderId(context.message()));
        args.put("customerPhone", context.customerPhone());
        args.put("productKeyword", keyword(context.message()));
        args.put("timeHint", timeHint(context.message()));
        return args;
    }

    private String policyQuery(PlanContext context, Intent intent) {
        String message = context.message();
        if (UserIntent.contains(message, "运费")) {
            return "退货运费谁承担";
        }
        if (UserIntent.contains(message, "丢")) {
            return "物流丢件 退款";
        }
        if (UserIntent.packagingClaim(message)) {
            return "物流破损 签收 24 小时";
        }
        if (UserIntent.humanDamageClaim(message)) {
            return "人为损坏 进水 保修";
        }
        if (UserIntent.contains(message, "激活", "会员")) {
            return "已激活 数字商品 七天无理由";
        }
        if (UserIntent.contains(message, "坚果", "食品", "变质")) {
            return "食品 七天无理由 签收当日变质";
        }
        if (UserIntent.contains(message, "缺", "充电线", "配件")) {
            return "配件缺失 七天无理由";
        }
        if (intent == Intent.EXCHANGE || UserIntent.qualityComplaint(message)) {
            return "十五天质量问题换货 保修期与维修";
        }
        if (intent == Intent.WARRANTY) {
            return "保修期与维修 人为损坏";
        }
        if (intent == Intent.POLICY) {
            return message;
        }
        return "七天无理由退货 签收 " + keyword(message);
    }

    static String extractOrderId(String message) {
        if (message == null) {
            return "";
        }
        Matcher matcher = ORDER_ID.matcher(message);
        return matcher.find() ? matcher.group() : "";
    }

    static String keyword(String message) {
        if (message == null) {
            return "";
        }
        if (message.contains("耳机")) {
            return "耳机";
        }
        if (message.contains("T恤") || message.contains("T 恤")) {
            return "T 恤";
        }
        if (message.contains("坚果")) {
            return "坚果";
        }
        if (message.contains("背心")) {
            return "背心";
        }
        if (message.contains("会员")) {
            return "会员";
        }
        return "";
    }

    static String timeHint(String message) {
        if (message != null && UserIntent.contains(message, "上周", "最近", "这周", "前几天")) {
            return "last_week";
        }
        return "";
    }

    private static PlanStep step(String id, AgentAction action, Map<String, Object> args, String purpose) {
        return new PlanStep(id, action.wire(), args, purpose);
    }

    private static String nextId(List<PlanStep> steps) {
        return "s" + (steps.size() + 1);
    }
}

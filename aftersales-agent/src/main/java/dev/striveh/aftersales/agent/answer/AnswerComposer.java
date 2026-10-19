package dev.striveh.aftersales.agent.answer;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import dev.striveh.aftersales.agent.eligibility.DecisionCode;
import dev.striveh.aftersales.agent.eligibility.EligibilityDecision;
import dev.striveh.aftersales.agent.plan.ExecutionState;
import dev.striveh.aftersales.agent.retrieval.PolicyChunk;
import dev.striveh.aftersales.agent.retrieval.PolicyIndex;
import dev.striveh.aftersales.common.Logistics;
import dev.striveh.aftersales.common.Order;

public final class AnswerComposer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA);

    private final PolicyIndex index;

    public AnswerComposer(PolicyIndex index) {
        this.index = index;
    }

    public String compose(ExecutionState state, LocalDate today) {
        List<Citation> citations = citations(state.decision(), state.clauses());
        state.citations(citations);
        String text = render(state, today, citations);
        if (state.pending() != null) {
            text = text + "退款申请已生成，尚未打款，需要人工确认。金额 " + yuan(state.pending().amount()) + "。";
        }
        return text;
    }

    public List<Citation> citations(EligibilityDecision decision, List<PolicyChunk> retrieved) {
        if (decision == null || decision.sectionTitle() == null || decision.docId() == null) {
            return List.of();
        }
        Optional<PolicyChunk> match = retrieved.stream()
                .filter(chunk -> chunk.docId().equals(decision.docId()) && chunk.sectionTitle().contains(decision.sectionTitle()))
                .findFirst();
        if (match.isEmpty()) {
            match = index.findBySection(decision.docId(), decision.sectionTitle());
        }
        return match.map(chunk -> List.of(new Citation(
                chunk.docId(),
                chunk.docTitle(),
                chunk.sectionId(),
                chunk.sectionTitle(),
                quote(chunk.content())))).orElse(List.of());
    }

    private String render(ExecutionState state, LocalDate today, List<Citation> citations) {
        EligibilityDecision decision = state.decision();
        if (decision == null) {
            return "我还没有完成判定。请补充订单号或下单手机号。";
        }
        String cite = cite(citations);
        Order order = decision.order() != null ? decision.order() : state.order();
        return switch (decision.code()) {
            case SMALL_TALK -> "你好，我是澄光数码的售后助手。可以帮你查订单、物流，并按退换货、保修和物流制度说明能不能退。退款需要你确认后才会提交。";
            case OUT_OF_SCOPE -> "发票和招聘不在这个售后助手的处理范围内。退换货、物流和保修可以继续问我。";
            case LOOKUP_FAILED -> "我没有查到对应订单。请核对订单号，或换下单手机号再试一次。";
            case NEED_CLARIFICATION -> clarification(state);
            case POLICY_INFO -> "按" + cite + (citations.isEmpty() ? "相关制度" : "：" + citations.get(0).quote());
            case LOGISTICS_INFO -> logistics(order, state.logistics()) + cite;
            case RETURN_NO_REASON -> named(order) + signLine(order, today) + "仍在七天无理由退货期内。" + cite
                    + "耳机类拆封不影响这项权益，但配件要齐全、不能有人为损坏。这笔订单符合退货条件。"
                    + (state.pending() == null ? "我没有发起退款。" : "");
            case RETURN_NOT_SIGNED -> named(order) + "包裹还没签收。" + cite + "未签收可以拦截并退回实付货款。我没有发起退款。";
            case RETURN_LOST_IN_TRANSIT -> named(order) + "物流轨迹是丢件。" + cite + "丢件可以全额退回实付货款，不需要先把商品寄回。我没有发起退款。";
            case RETURN_TRANSIT_DAMAGE -> named(order) + signLine(order, today) + "外包装破损还在签收后 24 小时内。" + cite
                    + "可以退回实付货款。我没有发起退款。";
            case RETURN_FOOD_QUALITY -> named(order) + "食品在签收当日出现变质。" + cite + "可以退回实付货款。我没有发起退款。";
            case EXCHANGE_QUALITY -> named(order) + signLine(order, today) + "这是非人为质量问题，而且仍在十五天内。" + cite
                    + "制度要求优先换货，不直接退款。";
            case WARRANTY_REPAIR -> named(order) + signLine(order, today) + "已经超过十五天换货窗口，但仍在 12 个月保修期内。" + cite
                    + "可以免费检测维修，不能直接退款。";
            case DENIED_WINDOW -> named(order) + signLine(order, today) + decision.summary() + "。" + cite;
            case DENIED_EXCLUDED_CATEGORY -> named(order) + "类目是" + order.category().label() + "。" + cite + decision.summary() + "。";
            case DENIED_MISSING_PARTS -> named(order) + "仓储备注是配件不齐。" + cite + "缺少配件时不能走七天无理由。";
            case DENIED_HUMAN_DAMAGE -> named(order) + "损坏原因属于进水或摔坏。" + cite + "人为损坏不保修，也不能改走无理由退货。";
            case DENIED_ALREADY_REFUNDED -> named(order) + "状态已经是已退款。" + cite + "已经退款的订单不能再次退款。";
        };
    }

    private String clarification(ExecutionState state) {
        if (state.candidates().isEmpty()) {
            return "我还不能定位订单。请提供订单号或下单手机号。";
        }
        StringBuilder text = new StringBuilder("这个手机号下有多笔可能相关的订单，请告诉我要看哪一笔：");
        for (Order candidate : state.candidates()) {
            text.append(candidate.orderId())
                    .append(" ")
                    .append(candidate.productName())
                    .append("，")
                    .append(candidate.signedOn() == null ? candidate.status().label() : "签收于 " + candidate.signedOn().format(DATE))
                    .append("。");
        }
        return text.toString();
    }

    private String logistics(Order order, Logistics logistics) {
        if (order == null || logistics == null) {
            return "没有查到物流轨迹。";
        }
        String latest = logistics.events().isEmpty() ? logistics.statusText()
                : logistics.events().get(logistics.events().size() - 1).description();
        return named(order) + "承运商是" + logistics.carrier() + "，运单 " + logistics.trackingNo() + "，当前是"
                + logistics.statusText() + "。最近一条轨迹：" + latest + "。";
    }

    private static String named(Order order) {
        if (order == null) {
            return "";
        }
        return order.customerName() + "，订单 " + order.orderId() + "（" + order.productName() + "，实付 " + yuan(order.amount()) + "）。";
    }

    private static String signLine(Order order, LocalDate today) {
        if (order == null || order.signedOn() == null) {
            return "";
        }
        long days = ChronoUnit.DAYS.between(order.signedOn(), today);
        return "签收日是" + order.signedOn().format(DATE) + "，距今天 " + days + " 天。";
    }

    private static String cite(List<Citation> citations) {
        if (citations.isEmpty()) {
            return "";
        }
        Citation citation = citations.get(0);
        return "依据《" + citation.docTitle() + "》「" + citation.sectionTitle() + "」。";
    }

    public static String yuan(java.math.BigDecimal amount) {
        return "¥" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    static String quote(String content) {
        String withoutHeading = content.replaceFirst("(?s)^#+[^\\n]*\\n+", "");
        String flat = withoutHeading.replaceAll("\\s+", "");
        if (flat.length() > 90) {
            return flat.substring(0, 90) + "…";
        }
        return flat;
    }
}

package dev.striveh.aftersales.agent.eligibility;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import dev.striveh.aftersales.agent.retrieval.PolicySections;
import dev.striveh.aftersales.common.Order;
import dev.striveh.aftersales.common.OrderStatus;
import dev.striveh.aftersales.common.ProductCategory;

/**
 * Money and eligibility decisions are code, not a model sample. The model may
 * plan and phrase the reply; this class decides which clause applies.
 */
public final class EligibilityDecider {

    public EligibilityDecision decide(DecisionFacts facts) {
        Intent intent = facts.intent();
        if (intent == Intent.SMALL_TALK) {
            return plain(DecisionCode.SMALL_TALK, "寒暄");
        }
        if (intent == Intent.OUT_OF_SCOPE) {
            return plain(DecisionCode.OUT_OF_SCOPE, "超出售后范围");
        }
        if (facts.lookupFailed()) {
            return plain(DecisionCode.LOOKUP_FAILED, "没有查到订单");
        }
        if (facts.order() == null && facts.candidates() != null && facts.candidates().size() > 1) {
            return plain(DecisionCode.NEED_CLARIFICATION, "匹配到多笔订单");
        }
        if (intent == Intent.POLICY || facts.order() == null && intent == Intent.POLICY) {
            return policy(facts.message());
        }
        if (facts.order() == null && (intent == Intent.RETURN || intent == Intent.REFUND || intent == Intent.EXCHANGE
                || intent == Intent.WARRANTY || intent == Intent.LOGISTICS)) {
            return plain(DecisionCode.NEED_CLARIFICATION, "还没有定位到订单");
        }
        if (facts.order() == null) {
            return policy(facts.message());
        }
        if (intent == Intent.LOGISTICS) {
            return logistics(facts.order());
        }
        return decideOrder(facts);
    }

    private EligibilityDecision decideOrder(DecisionFacts facts) {
        Order order = facts.order();
        LocalDate today = facts.today();
        String message = facts.message() == null ? "" : facts.message();
        long days = order.signedOn() == null ? -1 : ChronoUnit.DAYS.between(order.signedOn(), today);

        if (order.status() == OrderStatus.REFUNDED) {
            return deny(order, DecisionCode.DENIED_ALREADY_REFUNDED, PolicySections.DOC_RETURN, PolicySections.CONFIRM,
                    "订单已经退过款");
        }
        if (order.status() == OrderStatus.LOST) {
            return allow(order, DecisionCode.RETURN_LOST_IN_TRANSIT, PolicySections.DOC_LOGISTICS, PolicySections.LOST,
                    "物流丢件，可全额退款");
        }
        if (order.status() == OrderStatus.SHIPPED || order.signedOn() == null) {
            return allow(order, DecisionCode.RETURN_NOT_SIGNED, PolicySections.DOC_LOGISTICS, PolicySections.INTERCEPT,
                    "尚未签收，可拦截退款");
        }
        if (order.humanDamage() || UserIntent.humanDamageClaim(message)) {
            return deny(order, DecisionCode.DENIED_HUMAN_DAMAGE, PolicySections.DOC_WARRANTY, PolicySections.HUMAN,
                    "人为损坏，不保修也不走无理由退货");
        }
        if (order.shippingDamage() && UserIntent.packagingClaim(message) && days >= 0 && days <= 1) {
            return allow(order, DecisionCode.RETURN_TRANSIT_DAMAGE, PolicySections.DOC_LOGISTICS, PolicySections.DAMAGE,
                    "签收 24 小时内的物流破损，可退");
        }
        if (order.category() == ProductCategory.FOOD) {
            if (UserIntent.qualityComplaint(message) && days == 0) {
                return allow(order, DecisionCode.RETURN_FOOD_QUALITY, PolicySections.DOC_RETURN, PolicySections.FOOD,
                        "食品签收当日变质，可退");
            }
            return deny(order, DecisionCode.DENIED_EXCLUDED_CATEGORY, PolicySections.DOC_RETURN, PolicySections.EXCLUDED,
                    "食品不支持七天无理由");
        }
        if (order.category() == ProductCategory.CUSTOM || order.category() == ProductCategory.INTIMATE) {
            return deny(order, DecisionCode.DENIED_EXCLUDED_CATEGORY, PolicySections.DOC_RETURN, PolicySections.EXCLUDED,
                    order.category().label() + "不支持七天无理由");
        }
        if (order.category() == ProductCategory.DIGITAL) {
            if (!order.activated() && days >= 0 && days <= 7) {
                return allow(order, DecisionCode.RETURN_NO_REASON, PolicySections.DOC_RETURN, PolicySections.NO_REASON,
                        "未激活的数字商品仍在七天内，可退");
            }
            return deny(order, DecisionCode.DENIED_EXCLUDED_CATEGORY, PolicySections.DOC_RETURN, PolicySections.EXCLUDED,
                    "已激活的数字商品不支持无理由退货");
        }

        boolean quality = UserIntent.qualityComplaint(message);
        if (!order.accessoriesComplete() && !quality) {
            return deny(order, DecisionCode.DENIED_MISSING_PARTS, PolicySections.DOC_RETURN, PolicySections.PARTS,
                    "配件不齐，不能走七天无理由");
        }
        if (quality && days >= 0 && days <= 15) {
            return new EligibilityDecision(DecisionCode.EXCHANGE_QUALITY, false, PolicySections.DOC_RETURN,
                    PolicySections.EXCHANGE, "十五天内的质量问题，优先换货，不直接退款", order);
        }
        if (quality && inWarranty(order, today)) {
            return new EligibilityDecision(DecisionCode.WARRANTY_REPAIR, false, PolicySections.DOC_WARRANTY,
                    PolicySections.WARRANTY, "已过十五天换货窗口，保修期内可维修，不退款", order);
        }
        if (quality) {
            return deny(order, DecisionCode.DENIED_WINDOW, PolicySections.DOC_WARRANTY, PolicySections.BOUNDARY,
                    "已过保修期");
        }
        if (days >= 0 && days <= 7) {
            return allow(order, DecisionCode.RETURN_NO_REASON, PolicySections.DOC_RETURN, PolicySections.NO_REASON,
                    "仍在七天无理由退货期内");
        }
        if (inWarranty(order, today)) {
            return deny(order, DecisionCode.DENIED_WINDOW, PolicySections.DOC_RETURN, PolicySections.NO_REASON,
                    "已超过七天无理由，保修期内的非人为故障可以维修");
        }
        return deny(order, DecisionCode.DENIED_WINDOW, PolicySections.DOC_WARRANTY, PolicySections.BOUNDARY,
                "已超过七天无理由和保修期");
    }

    private EligibilityDecision logistics(Order order) {
        if (order.status() == OrderStatus.LOST) {
            return new EligibilityDecision(DecisionCode.LOGISTICS_INFO, false, PolicySections.DOC_LOGISTICS,
                    PolicySections.LOST, "物流显示丢件", order);
        }
        if (order.signedOn() == null) {
            return new EligibilityDecision(DecisionCode.LOGISTICS_INFO, false, PolicySections.DOC_LOGISTICS,
                    PolicySections.INTERCEPT, "尚未签收", order);
        }
        return new EligibilityDecision(DecisionCode.LOGISTICS_INFO, false, PolicySections.DOC_LOGISTICS,
                PolicySections.PROOF, "已有签收轨迹", order);
    }

    private EligibilityDecision policy(String message) {
        String text = message == null ? "" : message;
        if (UserIntent.contains(text, "运费")) {
            return cite(PolicySections.DOC_RETURN, PolicySections.FREIGHT, "说明退货运费");
        }
        if (UserIntent.contains(text, "丢件", "丢失")) {
            return cite(PolicySections.DOC_LOGISTICS, PolicySections.LOST, "说明丢件");
        }
        if (UserIntent.contains(text, "破损", "24")) {
            return cite(PolicySections.DOC_LOGISTICS, PolicySections.DAMAGE, "说明物流破损");
        }
        if (UserIntent.contains(text, "保修", "多久")) {
            return cite(PolicySections.DOC_WARRANTY, PolicySections.WARRANTY, "说明保修期");
        }
        if (UserIntent.contains(text, "食品", "生鲜")) {
            return cite(PolicySections.DOC_RETURN, PolicySections.EXCLUDED, "说明食品排除");
        }
        return cite(PolicySections.DOC_RETURN, PolicySections.NO_REASON, "说明七天无理由");
    }

    private static boolean inWarranty(Order order, LocalDate today) {
        if (order.signedOn() == null) {
            return false;
        }
        LocalDate end = order.signedOn().plusDays(1).plusMonths(12);
        return !today.isAfter(end);
    }

    private static EligibilityDecision allow(Order order, DecisionCode code, String docId, String section, String summary) {
        return new EligibilityDecision(code, true, docId, section, summary, order);
    }

    private static EligibilityDecision deny(Order order, DecisionCode code, String docId, String section, String summary) {
        return new EligibilityDecision(code, false, docId, section, summary, order);
    }

    private static EligibilityDecision plain(DecisionCode code, String summary) {
        return new EligibilityDecision(code, false, null, null, summary, null);
    }

    private static EligibilityDecision cite(String docId, String section, String summary) {
        return new EligibilityDecision(DecisionCode.POLICY_INFO, false, docId, section, summary, null);
    }

    public static List<Order> emptyCandidates() {
        return List.of();
    }
}

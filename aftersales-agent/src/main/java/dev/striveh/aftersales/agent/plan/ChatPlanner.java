package dev.striveh.aftersales.agent.plan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

public final class ChatPlanner {

    private static final Logger log = LoggerFactory.getLogger(ChatPlanner.class);

    private final ChatClient chatClient;
    private final PlanParser parser;
    private final HeuristicPlans fallback;
    private final boolean allowFallback;

    public ChatPlanner(ChatModel model, PlanParser parser, HeuristicPlans fallback, boolean allowFallback) {
        this.chatClient = ChatClient.builder(model).build();
        this.parser = parser;
        this.fallback = fallback;
        this.allowFallback = allowFallback;
    }

    public Planned plan(PlanContext context) {
        return complete("PLAN", context, "", "");
    }

    public Planned replan(PlanContext context, String failedAction, String failureMessage) {
        return complete("REPLAN", context, failedAction, failureMessage);
    }

    private Planned complete(String task, PlanContext context, String failedAction, String failureMessage) {
        String prompt = prompt(task, context, failedAction, failureMessage);
        String content = chatClient.prompt().user(prompt).call().content();
        try {
            return new Planned(parser.parse(content), false);
        } catch (RuntimeException exception) {
            if (!allowFallback) {
                throw exception;
            }
            log.warn("计划 JSON 无法解析，改用确定性规划。{}", exception.getMessage());
            Plan plan = "REPLAN".equals(task)
                    ? fallback.replan(context, failedAction, failureMessage)
                    : fallback.plan(context);
            return new Planned(plan, true);
        }
    }

    static String prompt(String task, PlanContext context, String failedAction, String failureMessage) {
        return """
                你是澄光数码售后 Agent 的规划器。只输出一个 JSON 对象，不要写解释，不要使用 Markdown。
                允许的 action 只有：lookup_order、lookup_logistics、retrieve_policy、decide_eligibility、request_refund、draft_answer。
                lookup_order 的 args 使用 orderId、customerPhone、productKeyword、timeHint。timeHint 只有上周或最近才填 last_week。
                依赖上一步订单号时，写成 $s1.orderId。
                用户只是问能不能退时，不要加入 request_refund。只有「帮我退款」「申请退款」才加入 request_refund。
                request_refund 不会打款。
                如果订单号查不到，重新规划时去掉 orderId，改用手机号。

                TASK: %s
                CUSTOMER_PHONE: %s
                USER_MESSAGE: %s
                KNOWN_ORDER_ID: %s
                PREVIOUS_REFUNDABLE: %s
                IGNORE_ORDER_ID: %s
                FAILED_ACTION: %s
                FAILURE_MESSAGE: %s
                """.formatted(task, context.customerPhone(), oneLine(context.message()), context.knownOrderId(),
                context.previousRefundable(), context.ignoreOrderIdInMessage(), nullToEmpty(failedAction),
                oneLine(failureMessage));
    }

    private static String oneLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

package dev.striveh.aftersales.agent.retrieval;

public final class PolicySections {

    public static final String DOC_RETURN = "return-policy";
    public static final String DOC_WARRANTY = "warranty-policy";
    public static final String DOC_LOGISTICS = "logistics-policy";

    public static final String NO_REASON = "七天无理由退货";
    public static final String EXCLUDED = "不支持七天无理由的类目";
    public static final String FOOD = "食品签收当日变质";
    public static final String EXCHANGE = "十五天质量问题换货";
    public static final String PARTS = "配件缺失";
    public static final String FREIGHT = "退货运费";
    public static final String CONFIRM = "退款需要人工确认";
    public static final String SIGNED_AT = "签收时间的认定";
    public static final String WARRANTY = "保修期与维修";
    public static final String HUMAN = "人为损坏";
    public static final String BOUNDARY = "保修与退货的边界";
    public static final String INTERCEPT = "未签收拦截";
    public static final String LOST = "物流丢件";
    public static final String DAMAGE = "物流破损";
    public static final String PROOF = "签收凭证";

    private PolicySections() {
    }
}

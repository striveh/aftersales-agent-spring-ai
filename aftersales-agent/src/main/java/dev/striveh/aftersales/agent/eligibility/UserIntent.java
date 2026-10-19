package dev.striveh.aftersales.agent.eligibility;

public final class UserIntent {

    private UserIntent() {
    }

    public static Intent classify(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return Intent.SMALL_TALK;
        }
        boolean personal = contains(text, "我", "订单", "这单", "这个", "帮我");
        if (text.length() <= 8 && contains(text, "你好", "您好", "在吗", "谢谢", "hello", "hi")) {
            return Intent.SMALL_TALK;
        }
        if (contains(text, "发票", "开票", "招聘")) {
            return Intent.OUT_OF_SCOPE;
        }
        if (!personal && contains(text, "政策", "条款", "无理由", "运费", "保修期", "一般多久", "拆封", "谁出")) {
            return Intent.POLICY;
        }
        if (contains(text, "帮我退款", "申请退款", "给我退款", "办理退款", "发起退款", "帮我退")) {
            return Intent.REFUND;
        }
        if (contains(text, "换货", "能换", "可以换", "更换")) {
            return Intent.EXCHANGE;
        }
        if (contains(text, "保修", "维修", "过保")) {
            return Intent.WARRANTY;
        }
        if (contains(text, "到哪", "物流", "快递", "发货", "签收了吗", "运单") && !contains(text, "退", "换", "赔")) {
            return Intent.LOGISTICS;
        }
        if (contains(text, "退", "坏", "破", "丢", "没声音", "故障", "变质", "进水", "激活")) {
            return Intent.RETURN;
        }
        if (personal) {
            return Intent.RETURN;
        }
        return Intent.POLICY;
    }

    public static boolean qualityComplaint(String message) {
        if (contains(message, "外包装", "包装破", "箱子破")) {
            return contains(message, "没声音", "故障", "无法充电", "异响", "无法开机", "变质");
        }
        return contains(message, "没声音", "故障", "质量问题", "坏了", "无法充电", "异响", "不工作", "变质", "发霉", "漏液", "无法开机");
    }

    public static boolean humanDamageClaim(String message) {
        return contains(message, "进水", "摔", "泡水", "压坏", "私自拆");
    }

    public static boolean packagingClaim(String message) {
        return contains(message, "外包装", "包装", "箱子", "物流破损");
    }

    public static boolean contains(String text, String... needles) {
        if (text == null) {
            return false;
        }
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

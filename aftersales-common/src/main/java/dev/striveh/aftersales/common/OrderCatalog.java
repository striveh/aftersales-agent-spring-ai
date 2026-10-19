package dev.striveh.aftersales.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock storefront. Dates are relative to "today" so the seven-day story stays true
 * whenever the demo is run. Order ids stay fixed so eval cases can name them.
 */
public final class OrderCatalog {

    public List<Order> load(LocalDate today) {
        List<Order> orders = new ArrayList<>();
        orders.add(earphone(today, "ORD-DEMO-1001", "13800001001", "林夏", 6, OrderStatus.SIGNED, true, false, false, "299"));
        orders.add(earphone(today, "ORD-DEMO-1002", "13800001002", "周予安", 40, OrderStatus.SIGNED, true, false, false, "299"));
        orders.add(order(today, "ORD-DEMO-1003", "13800001003", "陈麦", "定制姓名 T 恤", ProductCategory.CUSTOM,
                "129", 2, OrderStatus.SIGNED, true, false, false, false, "按买家姓名印制"));
        orders.add(earphone(today, "ORD-DEMO-1004", "13800001004", "赵澄", null, OrderStatus.SHIPPED, true, false, false, "259"));
        orders.add(earphone(today, "ORD-DEMO-1005", "13800001005", "孙泊", null, OrderStatus.LOST, true, false, false, "399"));
        orders.add(earphone(today, "ORD-DEMO-1006", "13800001006", "吴桐", 1, OrderStatus.SIGNED, true, false, true, "299"));
        orders.add(earphone(today, "ORD-DEMO-1007", "13800001007", "郑南星", 10, OrderStatus.SIGNED, true, false, false, "459"));
        orders.add(earphone(today, "ORD-DEMO-1008", "13800001008", "钱叙", 20, OrderStatus.SIGNED, true, true, false, "299"));
        orders.add(earphone(today, "ORD-DEMO-1009", "13800001009", "冯晚", 3, OrderStatus.SIGNED, false, false, false, "199"));
        orders.add(earphone(today, "ORD-DEMO-1010", "13800001010", "韩汐", 4, OrderStatus.REFUNDED, true, false, false, "299"));
        orders.add(earphone(today, "ORD-DEMO-1013", "13800001013", "许知夏", 7, OrderStatus.SIGNED, true, false, false, "329"));
        orders.add(earphone(today, "ORD-DEMO-1014", "13800001014", "沈见鹿", 8, OrderStatus.SIGNED, true, false, false, "329"));
        orders.add(earphone(today, "ORD-DEMO-1015A", "13800001015", "江宁", 6, OrderStatus.SIGNED, true, false, false, "299"));
        orders.add(earphone(today, "ORD-DEMO-1015B", "13800001015", "江宁", 40, OrderStatus.SIGNED, true, false, false, "199"));
        orders.add(order(today, "ORD-DEMO-1016", "13800001016", "叶微", "每日坚果礼盒", ProductCategory.FOOD,
                "89", 2, OrderStatus.SIGNED, true, false, false, false, ""));
        orders.add(order(today, "ORD-DEMO-1017", "13800001017", "罗小满", "莫代尔打底背心", ProductCategory.INTIMATE,
                "69", 1, OrderStatus.SIGNED, true, false, false, false, "贴身衣物"));
        orders.add(order(today, "ORD-DEMO-1018", "13800001018", "丁一", "澄光音乐会员年卡", ProductCategory.DIGITAL,
                "198", 1, OrderStatus.SIGNED, true, false, false, true, "权益已激活"));
        orders.add(order(today, "ORD-DEMO-1019", "13800001019", "梁果", "每日坚果礼盒", ProductCategory.FOOD,
                "89", 0, OrderStatus.SIGNED, true, false, false, false, "今日签收"));
        orders.add(earphone(today, "ORD-DEMO-1020", "13800001020", "蔡山", 5, OrderStatus.SIGNED, true, false, true, "299"));
        orders.add(order(today, "ORD-DEMO-1021", "13800001021", "丁二", "澄光音乐会员季卡", ProductCategory.DIGITAL,
                "68", 1, OrderStatus.SIGNED, true, false, false, false, "尚未激活"));
        return List.copyOf(orders);
    }

    public List<DemoCustomer> customers() {
        return List.of(
                new DemoCustomer("13800001001", "林夏", "耳机，6 天前签收", "我上周买的耳机能退吗"),
                new DemoCustomer("13800001002", "周予安", "耳机，40 天前签收，仍在保修期", "这副耳机没声音，能保修吗"),
                new DemoCustomer("13800001003", "陈麦", "定制 T 恤，2 天前签收", "我的定制 T 恤能退吗"),
                new DemoCustomer("13800001004", "赵澄", "耳机运输中，还没签收", "我的耳机到哪了"),
                new DemoCustomer("13800001005", "孙泊", "耳机物流丢件", "我的耳机包裹丢了，能退吗"),
                new DemoCustomer("13800001006", "吴桐", "昨天签收，外包装破损", "我的耳机外包装破了，能退吗"),
                new DemoCustomer("13800001007", "郑南星", "耳机 10 天前签收", "我的耳机没声音了，能换吗"),
                new DemoCustomer("13800001008", "钱叙", "检测备注为进水", "我的耳机进水了，能保修吗"),
                new DemoCustomer("13800001009", "冯晚", "缺少充电线", "我的耳机缺根线，能退吗"),
                new DemoCustomer("13800001010", "韩汐", "这笔订单已经退过款", "我那单耳机不是已经退过了吗"),
                new DemoCustomer("13800001013", "许知夏", "签收刚好满 7 天", "我这个耳机能退吗"),
                new DemoCustomer("13800001014", "沈见鹿", "签收第 8 天", "我这个耳机能退吗"),
                new DemoCustomer("13800001015", "江宁", "名下有两笔耳机订单", "我买的耳机能退吗"),
                new DemoCustomer("13800001016", "叶微", "食品，2 天前签收", "我买的坚果礼盒能退吗"),
                new DemoCustomer("13800001017", "罗小满", "贴身衣物，昨天签收", "我买的背心能退吗"),
                new DemoCustomer("13800001018", "丁一", "音乐会员已激活", "我的音乐会员能退吗"),
                new DemoCustomer("13800001019", "梁果", "食品，今天签收", "我今天收到的坚果变质了，能退吗"),
                new DemoCustomer("13800001020", "蔡山", "外包装破损，但已超过 24 小时", "我的耳机外包装破了，能退吗"),
                new DemoCustomer("13800001021", "丁二", "音乐会员尚未激活", "我的音乐会员还没激活，能退吗")
        );
    }

    private static Order earphone(LocalDate today, String id, String phone, String name, Integer signedDaysAgo,
                                  OrderStatus status, boolean accessories, boolean humanDamage, boolean shippingDamage,
                                  String amount) {
        return order(today, id, phone, name, "AirTune Pro 蓝牙耳机", ProductCategory.ELECTRONICS, amount,
                signedDaysAgo, status, accessories, humanDamage, shippingDamage, false, defaultNote(humanDamage, shippingDamage, accessories));
    }

    private static String defaultNote(boolean humanDamage, boolean shippingDamage, boolean accessories) {
        if (humanDamage) {
            return "检测结论：进水";
        }
        if (shippingDamage) {
            return "轨迹备注：外包装破损";
        }
        if (!accessories) {
            return "仓储备注：缺少充电线";
        }
        return "";
    }

    private static Order order(LocalDate today, String id, String phone, String name, String product,
                               ProductCategory category, String amount, Integer signedDaysAgo, OrderStatus status,
                               boolean accessories, boolean humanDamage, boolean shippingDamage, boolean activated,
                               String note) {
        LocalDate signed = signedDaysAgo == null ? null : today.minusDays(signedDaysAgo);
        LocalDate purchased = signed == null ? today.minusDays(status == OrderStatus.LOST ? 12 : 2) : signed.minusDays(1);
        if (status == OrderStatus.SHIPPED || status == OrderStatus.LOST) {
            signed = null;
        }
        return new Order(id, phone, name, product, category, new BigDecimal(amount), purchased, signed, status,
                accessories, humanDamage, shippingDamage, activated, note);
    }
}

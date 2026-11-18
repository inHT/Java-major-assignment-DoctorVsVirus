package game.system;

/**
 * 波次间商店的永久增益。初级/中级/高级互不依赖，只受金币限制。
 * 麻醉针按「降低敌方防御」处理（中/高级原文的「提升」视为笔误）。
 */
public enum ShopItem {
    SCALPEL_1("初级手术刀", "我方攻击力 +5%", 32, Kind.ATTACK, 0.05),
    SCALPEL_2("中级手术刀", "我方攻击力 +10%", 68, Kind.ATTACK, 0.10),
    SCALPEL_3("高级手术刀", "我方攻击力 +15%", 115, Kind.ATTACK, 0.15),
    BANDAGE_1("初级绷带", "护士治疗效率 +5%", 32, Kind.HEAL, 0.05),
    BANDAGE_2("中级绷带", "护士治疗效率 +10%", 68, Kind.HEAL, 0.10),
    BANDAGE_3("高级绷带", "护士治疗效率 +15%", 115, Kind.HEAL, 0.15),
    NEEDLE_1("初级麻醉针", "敌方防御力 -5%", 32, Kind.SHRED, 0.05),
    NEEDLE_2("中级麻醉针", "敌方防御力 -10%", 68, Kind.SHRED, 0.10),
    NEEDLE_3("高级麻醉针", "敌方防御力 -15%", 115, Kind.SHRED, 0.15);

    public enum Kind {
        ATTACK, HEAL, SHRED
    }

    private final String displayName;
    private final String desc;
    private final int price;
    private final Kind kind;
    private final double value;

    ShopItem(String displayName, String desc, int price, Kind kind, double value) {
        this.displayName = displayName;
        this.desc = desc;
        this.price = price;
        this.kind = kind;
        this.value = value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDesc() {
        return desc;
    }

    public int getPrice() {
        return price;
    }

    public Kind getKind() {
        return kind;
    }

    public double getValue() {
        return value;
    }
}

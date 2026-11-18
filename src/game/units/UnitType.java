package game.units;

import game.map.TileType;

import java.awt.Color;

/**
 * 己方医疗单位类型、平衡数值与技能参数。
 */
public enum UnitType {
    ORTHOPEDIC("骨科医生", "近战阻挡", "1",
            9, 10.5, 760, 72, 1.60, 1.28, 2,
            true, false, false, false,
            20.0, 6.0, "强化·射程+2",
            new Color(236, 244, 252), new Color(70, 110, 160)),
    RADIOLOGY("放射科医生", "远程物理", "2",
            16, 15.0, 380, 96, 1.35, 3.55, 0,
            false, true, false, false,
            24.0, 5.0, "强化·全体打击",
            new Color(255, 230, 120), new Color(210, 150, 20)),
    PROTECTIVE("防护服医生", "高防阻挡", "3",
            14, 18.0, 1320, 30, 1.40, 1.20, 3,
            true, false, false, false,
            26.0, 6.0, "无敌6秒",
            new Color(255, 170, 70), new Color(180, 90, 20)),
    NURSE("护士", "范围治疗", "4",
            11, 13.5, 340, 24, 0.85, 2.55, 0,
            false, false, true, false,
            22.0, 0.0, "全体治疗",
            new Color(255, 210, 220), new Color(200, 70, 100)),
    ANESTHESIA("麻醉科医生", "减速减攻", "5",
            12, 13.5, 360, 14, 1.55, 3.20, 0,
            false, true, false, false,
            24.0, 5.0, "全场减速",
            new Color(210, 180, 255), new Color(120, 70, 180)),
    SURGEON("外科医生", "高频近战", "6",
            9, 10.5, 700, 34, 0.75, 1.28, 2,
            true, false, false, false,
            20.0, 5.0, "破防30%",
            new Color(255, 200, 200), new Color(180, 60, 80)),
    INFO("信息科医生", "相邻强化", "7",
            13, 16.5, 280, 0, 1.00, 1.00, 0,
            false, false, false, true,
            70.0, 0.0, "清除全场",
            new Color(180, 230, 255), new Color(40, 120, 170));

    private final String displayName;
    private final String role;
    private final String hotkey;
    private final int cost;
    private final double redeploySeconds;
    private final double maxHp;
    private final double attack;
    private final double attackInterval;
    private final double rangeTiles;
    private final int maxBlock;
    private final boolean melee;
    private final boolean ranged;
    private final boolean healer;
    private final boolean support;
    private final double skillChargeSeconds;
    private final double skillDuration;
    private final String skillDesc;
    private final Color fill;
    private final Color accent;

    UnitType(String displayName, String role, String hotkey,
             int cost, double redeploySeconds, double maxHp, double attack,
             double attackInterval, double rangeTiles, int maxBlock,
             boolean melee, boolean ranged, boolean healer, boolean support,
             double skillChargeSeconds, double skillDuration, String skillDesc,
             Color fill, Color accent) {
        this.displayName = displayName;
        this.role = role;
        this.hotkey = hotkey;
        this.cost = cost;
        this.redeploySeconds = redeploySeconds;
        this.maxHp = maxHp;
        this.attack = attack;
        this.attackInterval = attackInterval;
        this.rangeTiles = rangeTiles;
        this.maxBlock = maxBlock;
        this.melee = melee;
        this.ranged = ranged;
        this.healer = healer;
        this.support = support;
        this.skillChargeSeconds = skillChargeSeconds;
        this.skillDuration = skillDuration;
        this.skillDesc = skillDesc;
        this.fill = fill;
        this.accent = accent;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public String getHotkey() {
        return hotkey;
    }

    public int getCost() {
        return cost;
    }

    public double getRedeploySeconds() {
        return redeploySeconds;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public double getAttack() {
        return attack;
    }

    public double getAttackInterval() {
        return attackInterval;
    }

    public double getRangeTiles() {
        return rangeTiles;
    }

    public int getMaxBlock() {
        return maxBlock;
    }

    public boolean isMelee() {
        return melee;
    }

    public boolean isRanged() {
        return ranged;
    }

    public boolean isHealer() {
        return healer;
    }

    public boolean isSupport() {
        return support;
    }

    public double getSkillChargeSeconds() {
        return skillChargeSeconds;
    }

    public double getSkillDuration() {
        return skillDuration;
    }

    public String getSkillDesc() {
        return skillDesc;
    }

    /** 技能发动立绘旁的简短说明，不含数值。 */
    public String getSkillAnnounceLine() {
        switch (this) {
            case ORTHOPEDIC:
                return "强化自身，拉长攻击距离";
            case RADIOLOGY:
                return "激光自天而降，打击全体敌人";
            case PROTECTIVE:
                return "展开护盾，短时间内不受伤害";
            case NURSE:
                return "为全场友军回复生命";
            case ANESTHESIA:
                return "全场敌人行动变得迟缓";
            case SURGEON:
                return "利刃环身，削弱敌方防御";
            case INFO:
                return "数据覆盖，清除全部敌人";
            default:
                return skillDesc;
        }
    }

    public Color getFill() {
        return fill;
    }

    public Color getAccent() {
        return accent;
    }

    public boolean canDeployOn(TileType tileType) {
        if (tileType == TileType.SPAWN || tileType == TileType.EXIT || tileType == TileType.WALL) {
            return false;
        }
        if (this == NURSE || this == INFO) {
            return tileType == TileType.PATH || tileType == TileType.HIGH;
        }
        if (melee) {
            return tileType == TileType.PATH;
        }
        return tileType == TileType.HIGH;
    }
}

package game.enemies;

import java.awt.Color;

/**
 * 敌方病毒类型及平衡数值。速度单位为像素/秒。
 */
public enum EnemyType {
    ECOLI("大肠杆菌", 390, 68, 22, 1.00, 6, 3, 1,
            new Color(120, 210, 80), new Color(40, 90, 30)),
    FLU("流感病毒", 550, 90, 26, 0.95, 8, 4, 1,
            new Color(90, 210, 230), new Color(20, 90, 120)),
    SMALLPOX("天花病毒", 1050, 54, 44, 1.00, 12, 6, 1,
            new Color(200, 170, 90), new Color(90, 70, 30)),
    COVID19("新冠病毒", 1350, 62, 56, 0.90, 15, 7, 1,
            new Color(220, 70, 70), new Color(90, 15, 20)),
    HIV("艾滋病病毒", 2000, 48, 68, 1.00, 18, 9, 1,
            new Color(150, 70, 190), new Color(50, 15, 80)),
    EBOLA("埃博拉病毒", 12000, 34, 116, 0.80, 45, 32, 2,
            new Color(140, 20, 30), new Color(40, 0, 8));

    private final String displayName;
    private final double maxHp;
    private final double speed;
    private final double attack;
    private final double attackInterval;
    private final int reward;
    private final int gold;
    private final int leakDamage;
    private final Color fill;
    private final Color stroke;

    EnemyType(String displayName, double maxHp, double speed, double attack,
              double attackInterval, int reward, int gold, int leakDamage,
              Color fill, Color stroke) {
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.speed = speed;
        this.attack = attack;
        this.attackInterval = attackInterval;
        this.reward = reward;
        this.gold = gold;
        this.leakDamage = leakDamage;
        this.fill = fill;
        this.stroke = stroke;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public double getSpeed() {
        return speed;
    }

    public double getAttack() {
        return attack;
    }

    public double getAttackInterval() {
        return attackInterval;
    }

    public int getReward() {
        return reward;
    }

    public int getGold() {
        return gold;
    }

    public int getLeakDamage() {
        return leakDamage;
    }

    public Color getFill() {
        return fill;
    }

    public Color getStroke() {
        return stroke;
    }
}

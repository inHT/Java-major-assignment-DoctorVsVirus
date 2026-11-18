package game.system;

import game.Config;

/**
 * 医疗资源点（部署费用）与金币。
 */
public class ResourceManager {

    private double dp;
    private double regenAcc;
    private int gold;

    public ResourceManager() {
        this.dp = Config.STARTING_DP;
        this.gold = Config.STARTING_GOLD;
    }

    public void update(double dt) {
        regenAcc += dt * Config.DP_PER_SECOND;
        while (regenAcc >= 1.0) {
            if (dp < Config.MAX_DP) {
                dp += 1;
            }
            regenAcc -= 1.0;
        }
    }

    public int getDp() {
        return (int) dp;
    }

    public int getGold() {
        return gold;
    }

    public boolean canAfford(int cost) {
        return dp >= cost;
    }

    public boolean trySpend(int cost) {
        if (dp < cost) {
            return false;
        }
        dp -= cost;
        return true;
    }

    public void add(double amount) {
        dp = Math.min(Config.MAX_DP, dp + amount);
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public boolean trySpendGold(int cost) {
        if (gold < cost) {
            return false;
        }
        gold -= cost;
        return true;
    }
}

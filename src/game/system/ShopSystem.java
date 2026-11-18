package game.system;

import game.Config;

import java.awt.Rectangle;
import java.util.EnumSet;

/**
 * 商店购买与永久加成汇总。
 */
public class ShopSystem {

    private static final int PANEL_W = 720;
    private static final int PANEL_H = 470;

    private final EnumSet<ShopItem> owned = EnumSet.noneOf(ShopItem.class);

    public boolean owns(ShopItem item) {
        return owned.contains(item);
    }

    public boolean tryBuy(ShopItem item, ResourceManager resources) {
        if (owned.contains(item)) {
            return false;
        }
        if (!resources.trySpendGold(item.getPrice())) {
            return false;
        }
        owned.add(item);
        return true;
    }

    public double attackBonus() {
        return sum(ShopItem.Kind.ATTACK);
    }

    public double healBonus() {
        return sum(ShopItem.Kind.HEAL);
    }

    public double shredBonus() {
        return sum(ShopItem.Kind.SHRED);
    }

    private double sum(ShopItem.Kind kind) {
        double total = 0;
        for (ShopItem item : owned) {
            if (item.getKind() == kind) {
                total += item.getValue();
            }
        }
        return total;
    }

    public static int panelX() {
        return (Config.WINDOW_WIDTH - PANEL_W) / 2;
    }

    public static int panelY() {
        return 70;
    }

    public static int panelW() {
        return PANEL_W;
    }

    public static int panelH() {
        return PANEL_H;
    }

    public static Rectangle itemRect(int index) {
        int col = index % 3;
        int row = index / 3;
        int x = panelX() + 24 + col * 228;
        int y = panelY() + 88 + row * 108;
        return new Rectangle(x, y, 214, 96);
    }

    public static Rectangle continueRect() {
        int w = 200;
        int h = 40;
        int x = panelX() + (PANEL_W - w) / 2;
        int y = panelY() + PANEL_H - 58;
        return new Rectangle(x, y, w, h);
    }
}

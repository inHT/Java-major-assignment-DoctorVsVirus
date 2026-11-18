package game.enemies;

import game.Config;
import game.map.Path;
import game.ui.RenderUtils;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 艾滋病病毒：对附近医生施加攻击力削弱光环。
 */
public class HIV extends Enemy {

    public static final double AURA_TILES = 2.3;
    public static final double ATTACK_FACTOR = 0.65;

    public HIV(Path path) {
        super(EnemyType.HIV, path);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        g.setColor(new Color(150, 70, 190, 40));
        int aura = (int) (AURA_TILES * Config.TILE_SIZE);
        g.fillOval((int) x - aura, (int) y - aura, aura * 2, aura * 2);
        RenderUtils.fillCircle(g, x, y, 13, type.getFill(), type.getStroke());
        g.setColor(type.getStroke());
        g.drawOval((int) x - 7, (int) y - 7, 14, 14);
        g.drawLine((int) x - 8, (int) y, (int) x + 8, (int) y);
        g.drawLine((int) x, (int) y - 8, (int) x, (int) y + 8);
    }

    @Override
    protected double bodyRadius() {
        return 13;
    }
}

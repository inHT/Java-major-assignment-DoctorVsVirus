package game.enemies;

import game.map.Path;
import game.ui.RenderUtils;

import java.awt.Graphics2D;

/**
 * 天花病毒：中等血量与速度的平衡型。
 */
public class Smallpox extends Enemy {

    public Smallpox(Path path) {
        super(EnemyType.SMALLPOX, path);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        RenderUtils.fillCircle(g, x, y, 12, type.getFill(), type.getStroke());
        g.setColor(type.getStroke());
        g.fillOval((int) x - 6, (int) y - 5, 5, 5);
        g.fillOval((int) x + 2, (int) y - 3, 4, 4);
        g.fillOval((int) x - 2, (int) y + 3, 5, 4);
    }

    @Override
    protected double bodyRadius() {
        return 12;
    }
}

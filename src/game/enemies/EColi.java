package game.enemies;

import game.map.Path;
import game.ui.RenderUtils;

import java.awt.Graphics2D;

/**
 * 大肠杆菌：最弱、数量多、移动较快。
 */
public class EColi extends Enemy {

    public EColi(Path path) {
        super(EnemyType.ECOLI, path);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        RenderUtils.fillCircle(g, x, y, 9, type.getFill(), type.getStroke());
        g.setColor(type.getStroke());
        g.drawLine((int) x + 8, (int) y, (int) x + 14, (int) y - 4);
        g.drawLine((int) x + 8, (int) y, (int) x + 14, (int) y + 4);
    }

    @Override
    protected double bodyRadius() {
        return 9;
    }
}

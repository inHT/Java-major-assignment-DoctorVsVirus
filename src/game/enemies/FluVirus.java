package game.enemies;

import game.map.Path;
import game.ui.RenderUtils;

import java.awt.Graphics2D;

/**
 * 流感病毒：较弱，移速突出。
 */
public class FluVirus extends Enemy {

    public FluVirus(Path path) {
        super(EnemyType.FLU, path);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        RenderUtils.fillCircle(g, x, y, 10, type.getFill(), type.getStroke());
        g.setColor(type.getStroke());
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0;
            g.drawLine((int) x, (int) y,
                    (int) (x + Math.cos(a) * 13),
                    (int) (y + Math.sin(a) * 13));
        }
    }

    @Override
    protected double bodyRadius() {
        return 10;
    }
}

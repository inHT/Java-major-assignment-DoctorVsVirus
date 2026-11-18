package game.enemies;

import game.map.Path;
import game.ui.RenderUtils;

import java.awt.Graphics2D;

/**
 * 新冠病毒：血量过半后加速突进。
 */
public class COVID19 extends Enemy {

    public COVID19(Path path) {
        super(EnemyType.COVID19, path);
    }

    @Override
    public void onDamaged() {
        if (!raged && hp <= type.getMaxHp() * 0.5) {
            setRaged(true);
        }
    }

    @Override
    protected void drawBody(Graphics2D g) {
        RenderUtils.fillCircle(g, x, y, 12, type.getFill(), type.getStroke());
        g.setColor(raged ? type.getFill().brighter() : type.getStroke());
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            int x2 = (int) (x + Math.cos(a) * 16);
            int y2 = (int) (y + Math.sin(a) * 16);
            g.fillOval(x2 - 3, y2 - 3, 6, 6);
            g.drawLine((int) x, (int) y, x2, y2);
        }
    }

    @Override
    protected double bodyRadius() {
        return 14;
    }
}

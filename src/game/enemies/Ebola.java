package game.enemies;

import game.map.Path;
import game.ui.RenderUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 埃博拉病毒：Boss，高血高攻，残血后狂暴。
 */
public class Ebola extends Enemy {

    public Ebola(Path path) {
        super(EnemyType.EBOLA, path);
    }

    @Override
    public void onDamaged() {
        if (!enraged && hp <= type.getMaxHp() * 0.35) {
            setEnraged(true);
        }
    }

    @Override
    protected void drawBody(Graphics2D g) {
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(type.getStroke());
        g.drawLine((int) x - 18, (int) y + 6, (int) x + 18, (int) y - 6);
        RenderUtils.fillCircle(g, x, y, 16, type.getFill(), type.getStroke());
        RenderUtils.fillCircle(g, x - 14, y + 5, 7, type.getFill(), type.getStroke());
        RenderUtils.fillCircle(g, x + 14, y - 5, 7, type.getFill(), type.getStroke());
        if (enraged) {
            g.setColor(new Color(255, 80, 40, 90));
            g.fillOval((int) x - 22, (int) y - 22, 44, 44);
        }
    }

    @Override
    protected double bodyRadius() {
        return 18;
    }
}

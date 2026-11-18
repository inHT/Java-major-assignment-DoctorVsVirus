package game.units;

import game.ui.RenderUtils;
import game.ui.Sprites;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 麻醉科医生：远程低伤害，命中后降低敌人移速与攻速。
 */
public class AnesthesiaDoc extends Unit {

    public AnesthesiaDoc(int col, int row, double x, double y) {
        super(UnitType.ANESTHESIA, col, row, x, y);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        if (Sprites.drawUnit(g, type, x, y, Sprites.MAP_HEIGHT)) {
            return;
        }
        RenderUtils.fillCircle(g, x, y, 15, type.getFill(), type.getAccent());
        g.setColor(type.getAccent());
        g.setStroke(new BasicStroke(2.2f));
        g.drawLine((int) x - 6, (int) y + 8, (int) x + 8, (int) y - 10);
        g.fillOval((int) x + 6, (int) y - 14, 8, 8);
        g.setColor(new Color(80, 40, 120));
        g.fillRect((int) x - 8, (int) y + 6, 6, 8);
    }
}

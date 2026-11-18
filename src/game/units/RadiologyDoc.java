package game.units;

import game.ui.RenderUtils;
import game.ui.Sprites;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 放射科医生：高台远程物理输出。
 */
public class RadiologyDoc extends Unit {

    public RadiologyDoc(int col, int row, double x, double y) {
        super(UnitType.RADIOLOGY, col, row, x, y);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        if (Sprites.drawUnit(g, type, x, y, Sprites.MAP_HEIGHT)) {
            return;
        }
        RenderUtils.fillCircle(g, x, y, 15, type.getFill(), type.getAccent());
        g.setColor(new Color(180, 120, 10));
        for (int i = 0; i < 3; i++) {
            double ang = i * Math.PI * 2 / 3;
            int x2 = (int) (x + Math.cos(ang) * 10);
            int y2 = (int) (y + Math.sin(ang) * 10);
            g.drawLine((int) x, (int) y, x2, y2);
        }
        RenderUtils.fillCircle(g, x, y, 4, Color.WHITE, type.getAccent());
    }
}

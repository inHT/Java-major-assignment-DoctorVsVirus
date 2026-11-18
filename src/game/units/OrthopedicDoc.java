package game.units;

import game.ui.RenderUtils;
import game.ui.Sprites;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 骨科医生：部署在路径上，近战输出并阻挡敌人。
 */
public class OrthopedicDoc extends Unit {

    public OrthopedicDoc(int col, int row, double x, double y) {
        super(UnitType.ORTHOPEDIC, col, row, x, y);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        if (Sprites.drawUnit(g, type, x, y, Sprites.MAP_HEIGHT)) {
            return;
        }
        RenderUtils.fillCircle(g, x, y, 16, type.getFill(), type.getAccent());
        g.setColor(new Color(245, 248, 255));
        g.fillRoundRect((int) x - 8, (int) y - 4, 16, 14, 4, 4);
        RenderUtils.drawMedicalCross(g, x, y + 2, 9, new Color(50, 110, 190));
        g.setColor(new Color(90, 90, 100));
        g.fillRoundRect((int) x + 10, (int) y - 10, 4, 16, 2, 2);
        g.fillOval((int) x + 8, (int) y - 14, 8, 8);
    }
}

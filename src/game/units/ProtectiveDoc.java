package game.units;

import game.ui.RenderUtils;
import game.ui.Sprites;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 防护服医生：高生命、可阻挡多名敌人，伤害较低。
 */
public class ProtectiveDoc extends Unit {

    public ProtectiveDoc(int col, int row, double x, double y) {
        super(UnitType.PROTECTIVE, col, row, x, y);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        if (Sprites.drawUnit(g, type, x, y, Sprites.MAP_HEIGHT)) {
            return;
        }
        RenderUtils.fillCircle(g, x, y, 18, type.getFill(), type.getAccent());
        g.setColor(new Color(255, 230, 180));
        g.fillOval((int) x - 7, (int) y - 12, 14, 12);
        g.setColor(new Color(40, 40, 50, 160));
        g.fillRect((int) x - 8, (int) y - 6, 16, 5);
        RenderUtils.drawMedicalCross(g, x, y + 5, 10, new Color(220, 50, 40));
    }
}

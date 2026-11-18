package game.units;

import game.ui.RenderUtils;
import game.ui.Sprites;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 信息科医生：不攻击、不阻挡，为相邻友军提供属性加成。
 */
public class InfoDoc extends Unit {

    public InfoDoc(int col, int row, double x, double y) {
        super(UnitType.INFO, col, row, x, y);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        if (Sprites.drawUnit(g, type, x, y, Sprites.MAP_HEIGHT)) {
            return;
        }
        RenderUtils.fillCircle(g, x, y, 14, type.getFill(), type.getAccent());
        g.setColor(new Color(30, 50, 80));
        g.fillRoundRect((int) x - 8, (int) y - 6, 16, 12, 3, 3);
        g.setColor(new Color(80, 220, 180));
        g.fillRect((int) x - 5, (int) y - 3, 10, 6);
    }
}

package game.units;

import game.ui.RenderUtils;
import game.ui.Sprites;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 外科医生：近战，攻速高于骨科、单次伤害低于骨科。
 */
public class SurgeonDoc extends Unit {

    public SurgeonDoc(int col, int row, double x, double y) {
        super(UnitType.SURGEON, col, row, x, y);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        if (Sprites.drawUnit(g, type, x, y, Sprites.MAP_HEIGHT)) {
            return;
        }
        RenderUtils.fillCircle(g, x, y, 15, type.getFill(), type.getAccent());
        g.setColor(new Color(220, 50, 70));
        g.fillRect((int) x + 6, (int) y - 12, 3, 18);
        g.fillOval((int) x + 4, (int) y - 16, 8, 8);
    }
}

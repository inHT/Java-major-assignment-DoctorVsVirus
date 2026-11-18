package game.units;

import game.ui.RenderUtils;
import game.ui.Sprites;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 护士：不主动攻击，持续治疗射程内友军。
 */
public class Nurse extends Unit {

    public Nurse(int col, int row, double x, double y) {
        super(UnitType.NURSE, col, row, x, y);
    }

    @Override
    protected void drawBody(Graphics2D g) {
        if (Sprites.drawUnit(g, type, x, y, Sprites.MAP_HEIGHT)) {
            return;
        }
        RenderUtils.fillCircle(g, x, y, 14, type.getFill(), type.getAccent());
        g.setColor(new Color(255, 250, 252));
        g.fillOval((int) x - 6, (int) y - 11, 12, 10);
        RenderUtils.drawMedicalCross(g, x, y + 3, 12, new Color(220, 40, 70));
        g.setColor(new Color(255, 120, 150, 70));
        g.drawOval((int) (x - 18), (int) (y - 18), 36, 36);
    }
}

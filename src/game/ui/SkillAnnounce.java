package game.ui;

import game.Config;
import game.units.UnitType;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 技能发动时屏幕右侧弹出的立绘与台词。
 */
public class SkillAnnounce {

    public final UnitType type;
    public double age;

    public SkillAnnounce(UnitType type) {
        this.type = type;
    }

    public boolean isDead() {
        return age >= Config.SKILL_ANNOUNCE;
    }

    public void update(double dt) {
        age += dt;
    }

    public void draw(Graphics2D g) {
        double t = age / Config.SKILL_ANNOUNCE;
        float alpha;
        double slide;
        if (t < 0.14) {
            double u = t / 0.14;
            double ease = 1 - (1 - u) * (1 - u);
            alpha = (float) ease;
            slide = (1 - ease) * 260;
        } else if (t < 0.78) {
            alpha = 1f;
            slide = 0;
        } else {
            double u = (t - 0.78) / 0.22;
            alpha = (float) Math.max(0, 1 - u);
            slide = u * u * 80;
        }
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.04f, alpha)));

        int right = Config.WINDOW_WIDTH - 12 + (int) slide;
        int panelW = 420;
        int panelH = 168;
        int px = right - panelW;
        int py = Config.TOP_BAR + 72;

        g.setPaint(new GradientPaint(px, py, new Color(8, 16, 28, 40),
                right, py, new Color(12, 28, 44, 230)));
        g.fill(new RoundRectangle2D.Double(px, py, panelW, panelH, 16, 16));
        g.setColor(type.getAccent());
        g.setStroke(new BasicStroke(2.4f));
        g.drawLine(px + 18, py + 18, px + 18, py + panelH - 18);

        int portraitH = 176;
        RenderUtils.glow(g, right - 78, py + panelH / 2.0, 70, RenderUtils.withAlpha(type.getAccent(), 110));
        Sprites.drawUnit(g, type, right - 78, py + panelH / 2.0 + 6, portraitH, 1f);

        g.setFont(Fonts.TITLE);
        g.setColor(new Color(255, 236, 180));
        g.drawString(type.getDisplayName() + "·技能发动", px + 32, py + 52);
        g.setFont(Fonts.UI);
        g.setColor(new Color(210, 225, 235));
        g.drawString(type.getSkillAnnounceLine(), px + 32, py + 86);

        g.setColor(new Color(255, 220, 120, 80));
        g.setStroke(new BasicStroke(1.2f));
        g.drawLine(px + 32, py + 104, px + 210, py + 104);

        g.setComposite(old);
    }
}

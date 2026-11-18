package game.ui;

import game.Config;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

/**
 * 左下角技能就绪提示。
 */
public class SkillToast {

    public final String text;
    public double age;

    public SkillToast(String text) {
        this.text = text;
    }

    public boolean isDead() {
        return age >= Config.SKILL_TOAST;
    }

    public void update(double dt) {
        age += dt;
    }

    public float alpha() {
        double t = age / Config.SKILL_TOAST;
        if (t < 0.12) {
            return (float) (t / 0.12);
        }
        if (t > 0.78) {
            return (float) Math.max(0, 1 - (t - 0.78) / 0.22);
        }
        return 1f;
    }

    public double slide() {
        double t = Math.min(1, age / 0.18);
        double ease = 1 - (1 - t) * (1 - t);
        return (1 - ease) * 70;
    }

    public void draw(Graphics2D g, int index) {
        float a = alpha();
        if (a <= 0.02f) {
            return;
        }
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        int w = 210;
        int h = 30;
        int x = 16 - (int) slide();
        int y = Config.TOP_BAR + Config.MAP_PIXEL_HEIGHT - 18 - (index + 1) * (h + 6);
        RenderUtils.glow(g, x + 16, y + h / 2.0, 16, new Color(255, 210, 80, (int) (90 * a)));
        RenderUtils.fillRound(g, x, y, w, h, 8, new Color(16, 28, 40, 220));
        RenderUtils.drawRound(g, x, y, w, h, 8, new Color(255, 214, 110), 1.3f);
        g.setColor(new Color(255, 214, 90));
        g.fillOval(x + 10, y + 11, 8, 8);
        g.setFont(Fonts.SMALL);
        g.setColor(new Color(230, 245, 255));
        g.drawString(text, x + 24, y + 20);
        g.setComposite(old);
    }
}

package game.ui;

import game.Config;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * 信息科清场：二进制数字铺满全屏，铺满后再消灭敌人。
 */
public final class BinaryWipeEffect {

    private static final int CELL = 16;

    private BinaryWipeEffect() {
    }

    public static double fillRatio(double age) {
        if (age < 0) {
            return 0;
        }
        if (age >= Config.BINARY_COVER) {
            return 1;
        }
        return age / Config.BINARY_COVER;
    }

    public static float overlayAlpha(double age) {
        if (age < 0 || age >= Config.BINARY_TOTAL) {
            return 0f;
        }
        if (age <= Config.BINARY_COVER + Config.BINARY_HOLD) {
            return 1f;
        }
        double u = (age - Config.BINARY_COVER - Config.BINARY_HOLD) / Config.BINARY_FADE;
        u = Math.max(0, Math.min(1, u));
        return (float) (1.0 - u * u);
    }

    public static void draw(Graphics2D g, int width, int height, double age) {
        float alpha = overlayAlpha(age);
        if (alpha <= 0.01f) {
            return;
        }
        double fill = fillRatio(age);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.55f));
        g.setColor(new Color(4, 12, 10));
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setFont(Fonts.MONO);
        FontMetrics fm = g.getFontMetrics();
        int ascent = fm.getAscent();
        for (int y = 0; y < height; y += CELL) {
            for (int x = 0; x < width; x += CELL) {
                double appear = appearThreshold(x, y);
                if (appear > fill) {
                    continue;
                }
                boolean one = ((x * 73856093) ^ (y * 19349663) ^ 0x5f3759df) < 0;
                float glow = 0.45f + (float) ((appear * 0.55));
                int green = (int) (140 + glow * 115);
                g.setColor(new Color(40, Math.min(255, green), 90));
                g.drawString(one ? "1" : "0", x + 2, y + ascent - 2);
            }
        }
        g.setComposite(old);
    }

    private static double appearThreshold(int x, int y) {
        int h = (x * 83492791) ^ (y * 39916801) ^ (x * 17 + y * 31);
        h = (h >>> 1) & 0xFFFF;
        return h / 65535.0;
    }
}

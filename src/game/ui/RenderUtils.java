package game.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;

/**
 * 几何绘制辅助方法。本项目不使用外部图片，所有美术均由代码绘制。
 */
public final class RenderUtils {

    private RenderUtils() {
    }

    public static void enableQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    public static void fillCircle(Graphics2D g, double cx, double cy, double radius, Color fill, Color stroke) {
        Ellipse2D.Double oval = new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2);
        g.setColor(fill);
        g.fill(oval);
        if (stroke != null) {
            g.setColor(stroke);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(oval);
        }
    }

    public static void fillRound(Graphics2D g, double x, double y, double w, double h, double arc, Color fill) {
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    public static void drawRound(Graphics2D g, double x, double y, double w, double h, double arc, Color stroke, float width) {
        g.setColor(stroke);
        g.setStroke(new BasicStroke(width));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, arc, arc));
    }

    public static void drawCenteredString(Graphics2D g, String text, int cx, int cy) {
        FontMetrics fm = g.getFontMetrics();
        int x = cx - fm.stringWidth(text) / 2;
        int y = cy - fm.getHeight() / 2 + fm.getAscent();
        g.drawString(text, x, y);
    }

    public static void drawHealthBar(Graphics2D g, double cx, double top, double width, double hpRatio, Color color) {
        double x = cx - width / 2.0;
        double ratio = Math.max(0, Math.min(1, hpRatio));
        g.setColor(new Color(8, 12, 18, 220));
        g.fillRoundRect((int) x, (int) top, (int) width, 5, 3, 3);
        Color bar = color;
        if (ratio < 0.35) {
            float p = pulse(8, 0);
            bar = new Color(Math.min(255, color.getRed() + (int) (50 * p)), color.getGreen(), color.getBlue());
        }
        int fill = (int) Math.max(ratio > 0 ? 2 : 0, width * ratio);
        if (fill > 0) {
            g.setColor(bar);
            g.fillRoundRect((int) x, (int) top, fill, 5, 3, 3);
            g.setColor(new Color(255, 255, 255, 80));
            g.fillRect((int) x + 1, (int) top + 1, Math.max(0, fill - 2), 1);
        }
    }

    public static void glow(Graphics2D g, double cx, double cy, double radius, Color color) {
        if (radius <= 1) {
            return;
        }
        int a = Math.max(0, Math.min(180, color.getAlpha()));
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float((float) cx, (float) cy),
                (float) radius,
                new float[] {0f, 1f},
                new Color[] {
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), a),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)
                });
        Paint old = g.getPaint();
        g.setPaint(paint);
        g.fill(new Ellipse2D.Double(cx - radius, cy - radius, radius * 2, radius * 2));
        g.setPaint(old);
    }

    public static void footShadow(Graphics2D g, double cx, double cy, double rx) {
        g.setColor(new Color(0, 0, 0, 78));
        g.fill(new Ellipse2D.Double(cx - rx, cy - rx * 0.32, rx * 2, rx * 0.64));
    }

    public static void sheen(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(new Color(255, 255, 255, 26));
        int sh = (int) Math.max(4, Math.min(16, h * 0.38));
        g.fillRoundRect((int) x + 3, (int) y + 2, Math.max(1, (int) w - 6), sh, 8, 8);
    }

    private static float pulse(double speed, double phase) {
        return VisualClock.pulse(speed, phase);
    }

    public static void drawMedicalCross(Graphics2D g, double cx, double cy, double size, Color color) {
        double t = size * 0.28;
        g.setColor(color);
        g.fillRect((int) (cx - t / 2), (int) (cy - size / 2), (int) t, (int) size);
        g.fillRect((int) (cx - size / 2), (int) (cy - t / 2), (int) size, (int) t);
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    /**
     * 增益向上绿箭头 / 减益向下红箭头。
     */
    public static void drawStatusArrow(Graphics2D g, double cx, double cy, boolean up, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        int x = (int) Math.round(cx);
        int y = (int) Math.round(cy);
        int[] xs;
        int[] ys;
        if (up) {
            g.setColor(new Color(70, 220, 120, a));
            xs = new int[] {x, x - 7, x + 7};
            ys = new int[] {y - 10, y + 6, y + 6};
        } else {
            g.setColor(new Color(230, 60, 70, a));
            xs = new int[] {x, x - 7, x + 7};
            ys = new int[] {y + 10, y - 6, y - 6};
        }
        g.fillPolygon(xs, ys, 3);
        g.setColor(new Color(255, 255, 255, a / 2));
        g.drawPolygon(xs, ys, 3);
    }
}

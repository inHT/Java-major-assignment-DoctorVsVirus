package game.system;

import java.awt.Color;

/**
 * 短时视觉特效（斩击、治疗十字、光环等）。
 */
public class VisualEffect {

    public enum Kind {
        SLASH, HEAL, BEAM, BURST, RING, SKY_LASER
    }

    public final Kind kind;
    public final double x;
    public final double y;
    public final double x2;
    public final double y2;
    public final Color color;
    public final double duration;
    public double age;

    public VisualEffect(Kind kind, double x, double y, Color color, double duration) {
        this(kind, x, y, x, y, color, duration);
    }

    public VisualEffect(Kind kind, double x, double y, double x2, double y2, Color color, double duration) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
        this.duration = duration;
    }

    public void update(double dt) {
        age += dt;
    }

    public boolean isDead() {
        return age >= duration;
    }

    public double progress() {
        return Math.min(1.0, age / duration);
    }
}

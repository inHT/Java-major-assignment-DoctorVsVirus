package game.system;

import java.awt.Color;

/**
 * 飘字：伤害、治疗、费用获取。
 */
public class FloatingText {

    public final String text;
    public final Color color;
    public double x;
    public double y;
    public double age;
    public final double duration;

    public FloatingText(String text, double x, double y, Color color) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.duration = 0.85;
    }

    public void update(double dt) {
        age += dt;
        y -= 28 * dt;
    }

    public boolean isDead() {
        return age >= duration;
    }

    public float alpha() {
        return (float) Math.max(0, 1.0 - age / duration);
    }
}

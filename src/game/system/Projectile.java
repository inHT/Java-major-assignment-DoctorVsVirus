package game.system;

import game.enemies.Enemy;

import java.awt.Color;

/**
 * 弹道：远程单位发射，轻微追踪目标。
 */
public class Projectile {

    private double x;
    private double y;
    private final Enemy target;
    private final double damage;
    private final boolean applySlow;
    private final Color color;
    private final double speed;
    private boolean dead;
    private final double[] trailX = new double[5];
    private final double[] trailY = new double[5];
    private int trailSize;
    private boolean impactReady;
    private double impactX;
    private double impactY;

    public Projectile(double x, double y, Enemy target, double damage, boolean applySlow, Color color) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.damage = damage;
        this.applySlow = applySlow;
        this.color = color;
        this.speed = applySlow ? 320 : 420;
    }

    public void update(double dt) {
        if (dead) {
            return;
        }
        if (target == null || !target.isAlive()) {
            dead = true;
            return;
        }
        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double dist = Math.hypot(dx, dy);
        double step = speed * dt;
        if (dist <= step + 8) {
            target.takeDamage(damage);
            if (applySlow) {
                target.applySlow(2.6);
            }
            impactX = target.getX();
            impactY = target.getY();
            impactReady = true;
            dead = true;
            return;
        }
        pushTrail();
        x += dx / dist * step;
        y += dy / dist * step;
    }

    private void pushTrail() {
        for (int i = trailX.length - 1; i > 0; i--) {
            trailX[i] = trailX[i - 1];
            trailY[i] = trailY[i - 1];
        }
        trailX[0] = x;
        trailY[0] = y;
        if (trailSize < trailX.length) {
            trailSize++;
        }
    }

    public int getTrailSize() {
        return trailSize;
    }

    public double getTrailX(int index) {
        return trailX[index];
    }

    public double getTrailY(int index) {
        return trailY[index];
    }

    public boolean pollImpact() {
        if (!impactReady) {
            return false;
        }
        impactReady = false;
        return true;
    }

    public double getImpactX() {
        return impactX;
    }

    public double getImpactY() {
        return impactY;
    }

    public boolean isDead() {
        return dead;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }
}

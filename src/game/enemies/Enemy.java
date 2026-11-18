package game.enemies;

import game.Config;
import game.map.Path;
import game.ui.Fonts;
import game.ui.RenderUtils;
import game.ui.VisualClock;
import game.units.Unit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/**
 * 敌人基类：沿路点插值移动，被阻挡时停止并攻击阻挡者。
 */
public abstract class Enemy {

    protected final EnemyType type;
    protected final List<double[]> waypoints;
    protected final double totalLength;
    protected double x;
    protected double y;
    protected double hp;
    protected int waypointIndex;
    protected double traveled;
    protected boolean alive = true;
    protected boolean leaked;
    protected Unit blocker;
    protected double attackTimer;
    protected double slowTimer;
    protected double attackSlowTimer;
    protected boolean raged;
    protected boolean enraged;
    protected double extraVuln;
    protected double markRemain;
    protected double markFade;

    protected Enemy(EnemyType type, Path path) {
        this.type = type;
        this.waypoints = path.getWaypoints();
        this.totalLength = Math.max(1, path.getTotalLength());
        double[] spawn = path.getSpawnPoint();
        this.x = spawn[0];
        this.y = spawn[1];
        this.hp = type.getMaxHp();
        this.waypointIndex = 0;
    }

    public static Enemy create(EnemyType type, Path path) {
        switch (type) {
            case ECOLI:
                return new EColi(path);
            case FLU:
                return new FluVirus(path);
            case SMALLPOX:
                return new Smallpox(path);
            case COVID19:
                return new COVID19(path);
            case HIV:
                return new HIV(path);
            case EBOLA:
                return new Ebola(path);
            default:
                throw new IllegalArgumentException("未知敌人: " + type);
        }
    }

    /**
     * 特殊能力钩子，子类可覆写。
     */
    public void onDamaged() {
    }

    public void update(double dt) {
        if (!alive || leaked) {
            return;
        }
        if (slowTimer > 0) {
            slowTimer -= dt;
        }
        if (attackSlowTimer > 0) {
            attackSlowTimer -= dt;
        }
        if (markRemain > 0) {
            markRemain -= dt;
        } else if (markFade > 0) {
            markFade -= dt;
        }
        if (blocker != null && (!blocker.isAlive() || distanceTo(blocker.getX(), blocker.getY()) > Config.TILE_SIZE * 1.05)) {
            blocker.unblock(this);
            blocker = null;
        }
        if (blocker != null) {
            attackTimer -= dt;
            if (attackTimer <= 0) {
                double interval = type.getAttackInterval() * getAttackIntervalFactor();
                blocker.takeDamage(getAttackDamage());
                attackTimer = interval;
            }
            return;
        }
        moveAlongPath(dt);
    }

    private void moveAlongPath(double dt) {
        double remaining = getCurrentSpeed() * dt;
        while (remaining > 0 && waypointIndex < waypoints.size() - 1) {
            double[] next = waypoints.get(waypointIndex + 1);
            double dx = next[0] - x;
            double dy = next[1] - y;
            double dist = Math.hypot(dx, dy);
            if (dist < 0.001) {
                waypointIndex++;
                continue;
            }
            if (dist <= remaining) {
                x = next[0];
                y = next[1];
                traveled += dist;
                remaining -= dist;
                waypointIndex++;
            } else {
                x += dx / dist * remaining;
                y += dy / dist * remaining;
                traveled += remaining;
                remaining = 0;
            }
        }
        if (waypointIndex >= waypoints.size() - 1) {
            leaked = true;
            alive = false;
        }
    }

    public void takeDamage(double amount) {
        if (!alive) {
            return;
        }
        hp -= amount * (1.0 + extraVuln);
        onDamaged();
        if (hp <= 0) {
            hp = 0;
            alive = false;
            if (blocker != null) {
                blocker.unblock(this);
                blocker = null;
            }
        }
    }

    public void applySlow(double duration) {
        slowTimer = Math.max(slowTimer, duration);
        attackSlowTimer = Math.max(attackSlowTimer, duration);
    }

    public void setExtraVuln(double extraVuln) {
        this.extraVuln = extraVuln;
    }

    public void showDebuffMark(double duration) {
        markRemain = Math.max(markRemain, duration);
        markFade = Config.ARROW_FADE;
    }

    public float markAlpha() {
        if (markRemain > 0) {
            return 1f;
        }
        if (markFade > 0) {
            return (float) Math.max(0, markFade / Config.ARROW_FADE);
        }
        return 0f;
    }

    public double getCurrentSpeed() {
        double speed = type.getSpeed();
        if (raged) {
            speed *= 1.70;
        }
        if (enraged) {
            speed *= 1.35;
        }
        if (slowTimer > 0) {
            speed *= 0.55;
        }
        return speed;
    }

    public double getAttackDamage() {
        double atk = type.getAttack();
        if (enraged) {
            atk *= 1.40;
        }
        return atk;
    }

    public double getAttackIntervalFactor() {
        return attackSlowTimer > 0 ? 1.45 : 1.0;
    }

    public double distanceTo(double tx, double ty) {
        return Math.hypot(tx - x, ty - y);
    }

    public double getPathProgress() {
        return traveled / totalLength;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean hasLeaked() {
        return leaked;
    }

    public EnemyType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getHp() {
        return hp;
    }

    public double getHpRatio() {
        return hp / type.getMaxHp();
    }

    public Unit getBlocker() {
        return blocker;
    }

    public void setBlocker(Unit blocker) {
        this.blocker = blocker;
    }

    public boolean isRaged() {
        return raged;
    }

    public void setRaged(boolean raged) {
        this.raged = raged;
    }

    public boolean isEnraged() {
        return enraged;
    }

    public void setEnraged(boolean enraged) {
        this.enraged = enraged;
    }

    public final void draw(Graphics2D g) {
        RenderUtils.footShadow(g, x, y + bodyRadius() * 0.65, bodyRadius() * 1.15);
        if (enraged) {
            RenderUtils.glow(g, x, y, bodyRadius() + 10, new Color(255, 60, 50, 90));
        }
        double bob = Math.sin(VisualClock.seconds() * 3.2 + traveled * 0.05) * 1.05;
        Graphics2D body = (Graphics2D) g.create();
        body.translate(0, bob);
        drawBody(body);
        body.dispose();
        double top = y - bodyRadius() - 8;
        g.setFont(Fonts.TINY);
        g.setColor(new Color(255, 245, 230));
        RenderUtils.drawCenteredString(g, type.getDisplayName(), (int) x, (int) (top - 8));
        Color bar = leaked ? Color.RED : type.getFill();
        RenderUtils.drawHealthBar(g, x, top, 22, getHpRatio(), bar);
        if (slowTimer > 0) {
            float spin = (float) ((VisualClock.seconds() * 28) % 16);
            java.awt.Stroke old = g.getStroke();
            g.setStroke(new java.awt.BasicStroke(1.6f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND,
                    1f, new float[] {4f, 4f}, spin));
            g.setColor(new Color(170, 130, 255, 170));
            g.drawOval((int) (x - 15), (int) (y - 15), 30, 30);
            g.setStroke(old);
        }
        float arrow = markAlpha();
        if (arrow > 0.02f) {
            RenderUtils.drawStatusArrow(g, x + 16, y - bodyRadius() - 4, false, arrow);
        }
    }

    protected abstract void drawBody(Graphics2D g);

    protected abstract double bodyRadius();
}

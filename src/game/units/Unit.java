package game.units;

import game.Config;
import game.enemies.Enemy;
import game.ui.Fonts;
import game.ui.RenderUtils;
import game.ui.Sprites;
import game.ui.VisualClock;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 己方单位基类：生命、阻挡、技能充能与通用绘制。
 */
public abstract class Unit {

    protected final UnitType type;
    protected final int col;
    protected final int row;
    protected final double x;
    protected final double y;
    protected double hp;
    protected double attackTimer;
    protected double attackMultiplier = 1.0;
    protected double healMultiplier = 1.0;
    protected boolean alive = true;
    protected final List<Enemy> blockedEnemies = new ArrayList<>();

    protected double skillCharge;
    protected double skillEffectRemain;
    protected double markRemain;
    protected double markFade;
    protected double healGlowRemain;
    protected boolean skillReadyAnnounced;

    protected Unit(UnitType type, int col, int row, double x, double y) {
        this.type = type;
        this.col = col;
        this.row = row;
        this.x = x;
        this.y = y;
        this.hp = type.getMaxHp();
    }

    public static Unit create(UnitType type, int col, int row, double x, double y) {
        switch (type) {
            case ORTHOPEDIC:
                return new OrthopedicDoc(col, row, x, y);
            case RADIOLOGY:
                return new RadiologyDoc(col, row, x, y);
            case PROTECTIVE:
                return new ProtectiveDoc(col, row, x, y);
            case NURSE:
                return new Nurse(col, row, x, y);
            case ANESTHESIA:
                return new AnesthesiaDoc(col, row, x, y);
            case SURGEON:
                return new SurgeonDoc(col, row, x, y);
            case INFO:
                return new InfoDoc(col, row, x, y);
            default:
                throw new IllegalArgumentException("未知单位: " + type);
        }
    }

    public void update(double dt, boolean canCharge) {
        if (!alive) {
            return;
        }
        pruneBlocked();
        if (attackTimer > 0) {
            attackTimer -= dt;
        }
        if (canCharge && skillCharge < type.getSkillChargeSeconds()) {
            skillCharge = Math.min(type.getSkillChargeSeconds(), skillCharge + dt);
        }
        if (skillEffectRemain > 0) {
            skillEffectRemain -= dt;
            if (skillEffectRemain < 0) {
                skillEffectRemain = 0;
            }
        }
        if (healGlowRemain > 0) {
            healGlowRemain = Math.max(0, healGlowRemain - dt);
        }
        updateMark(dt);
    }

    public void resetMultipliers() {
        attackMultiplier = 1.0;
        healMultiplier = 1.0;
    }

    public void applyAttackMul(double factor) {
        attackMultiplier *= factor;
    }

    public void applyHealMul(double factor) {
        healMultiplier *= factor;
    }

    public boolean tryAttackReady() {
        if (attackTimer <= 0) {
            attackTimer = type.getAttackInterval();
            return true;
        }
        return false;
    }

    public double getAttackDamage() {
        return type.getAttack() * attackMultiplier;
    }

    public double getHealAmount() {
        return type.getAttack() * attackMultiplier * healMultiplier;
    }

    public double getRangePixels() {
        return (type.getRangeTiles() + extraRangeTiles()) * Config.TILE_SIZE;
    }

    public double extraRangeTiles() {
        return type == UnitType.ORTHOPEDIC && skillEffectRemain > 0 ? 2.0 : 0;
    }

    public boolean isAoeActive() {
        return type == UnitType.RADIOLOGY && skillEffectRemain > 0;
    }

    public boolean isInvincible() {
        return type == UnitType.PROTECTIVE && skillEffectRemain > 0;
    }

    public boolean inRange(double tx, double ty) {
        return distanceTo(tx, ty) <= getRangePixels();
    }

    public double distanceTo(double tx, double ty) {
        return Math.hypot(tx - x, ty - y);
    }

    public boolean isAdjacentTo(Unit other) {
        return Math.abs(col - other.col) + Math.abs(row - other.row) == 1;
    }

    public boolean canBlock() {
        return type.getMaxBlock() > 0;
    }

    public boolean hasBlockCapacity() {
        return alive && blockedEnemies.size() < type.getMaxBlock();
    }

    public void block(Enemy enemy) {
        if (!blockedEnemies.contains(enemy)) {
            blockedEnemies.add(enemy);
        }
    }

    public void unblock(Enemy enemy) {
        blockedEnemies.remove(enemy);
    }

    public List<Enemy> getBlockedEnemies() {
        return blockedEnemies;
    }

    private void pruneBlocked() {
        Iterator<Enemy> it = blockedEnemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (e == null || !e.isAlive() || e.hasLeaked()) {
                it.remove();
            }
        }
    }

    public void takeDamage(double amount) {
        if (isInvincible()) {
            return;
        }
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
    }

    public void heal(double amount) {
        if (!alive) {
            return;
        }
        hp = Math.min(type.getMaxHp(), hp + amount);
        addHealGlow(0.45);
    }

    public void addHealGlow(double seconds) {
        healGlowRemain = Math.max(healGlowRemain, seconds);
    }

    public boolean pollSkillJustReady() {
        if (isSkillReady() && !skillReadyAnnounced) {
            skillReadyAnnounced = true;
            return true;
        }
        return false;
    }

    public boolean isSkillReady() {
        return alive && skillCharge >= type.getSkillChargeSeconds() - 0.0001;
    }

    public double getSkillChargeRatio() {
        return Math.min(1.0, skillCharge / type.getSkillChargeSeconds());
    }

    public void consumeSkill() {
        skillCharge = 0;
        skillReadyAnnounced = false;
        skillEffectRemain = type.getSkillDuration();
        if (type.getSkillDuration() > 0) {
            showBuffMark(type.getSkillDuration());
        } else {
            showBuffMark(1.15);
        }
    }

    public void showBuffMark(double duration) {
        markRemain = duration;
        markFade = Config.ARROW_FADE;
    }

    private void updateMark(double dt) {
        if (markRemain > 0) {
            markRemain -= dt;
        } else if (markFade > 0) {
            markFade -= dt;
        }
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

    public boolean isAlive() {
        return alive;
    }

    public UnitType getType() {
        return type;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
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

    public double getMaxHp() {
        return type.getMaxHp();
    }

    public double getHpRatio() {
        return hp / type.getMaxHp();
    }

    public final void draw(Graphics2D g) {
        boolean portrait = Sprites.hasPortrait(type);
        RenderUtils.footShadow(g, x, y + (portrait ? 22 : 12), portrait ? 15 : 11);
        double bob = Math.sin(VisualClock.seconds() * 2.4 + x * 0.07) * 1.15;
        Graphics2D body = (Graphics2D) g.create();
        body.translate(0, bob);
        drawHealGlow(body);
        drawBody(body);
        drawProtectiveShield(body);
        drawSurgeonSwords(body);
        body.dispose();
        double barY = portrait ? y - 32 : y - 22;
        RenderUtils.drawHealthBar(g, x, barY, portrait ? 34 : 28, getHpRatio(), new Color(80, 210, 120));
        drawSkillPip(g, barY - 7);
        if (canBlock() && !blockedEnemies.isEmpty()) {
            g.setFont(Fonts.TINY);
            g.setColor(new Color(255, 220, 80));
            String mark = blockedEnemies.size() + "/" + type.getMaxBlock();
            RenderUtils.drawCenteredString(g, mark, (int) x, (int) y + (portrait ? 28 : 20));
        }
        if (attackMultiplier < 0.99) {
            g.setColor(new Color(180, 80, 220, 160));
            g.fillOval((int) x + 8, (int) (portrait ? barY - 6 : y - 18), 7, 7);
        }
        float arrow = markAlpha();
        if (arrow > 0.02f) {
            RenderUtils.drawStatusArrow(g, x + 20, y - 18, true, arrow);
        }
    }

    private void drawHealGlow(Graphics2D g) {
        if (healGlowRemain <= 0) {
            return;
        }
        float a = (float) Math.min(1.0, healGlowRemain / 0.35);
        int pulse = (int) (22 + 6 * Math.sin(healGlowRemain * 14));
        g.setColor(new Color(70, 255, 140, (int) (70 * a)));
        g.fillOval((int) x - pulse, (int) y - pulse, pulse * 2, pulse * 2);
        g.setColor(new Color(180, 255, 200, (int) (140 * a)));
        g.fillOval((int) x - 10, (int) y - 10, 20, 20);
    }

    private void drawProtectiveShield(Graphics2D g) {
        if (!isInvincible()) {
            return;
        }
        double pulse = 26 + 3 * Math.sin(skillEffectRemain * 8);
        java.awt.Stroke old = g.getStroke();
        g.setStroke(new java.awt.BasicStroke(4.2f));
        g.setColor(new Color(120, 210, 255, 90));
        g.fillOval((int) (x - pulse), (int) (y - pulse), (int) (pulse * 2), (int) (pulse * 2));
        g.setColor(new Color(180, 240, 255, 210));
        g.drawOval((int) (x - pulse), (int) (y - pulse), (int) (pulse * 2), (int) (pulse * 2));
        g.setStroke(new java.awt.BasicStroke(1.6f));
        g.setColor(new Color(255, 255, 255, 160));
        g.drawOval((int) (x - pulse + 6), (int) (y - pulse + 6), (int) (pulse * 2 - 12), (int) (pulse * 2 - 12));
        g.setStroke(old);
    }

    private void drawSurgeonSwords(Graphics2D g) {
        if (type != UnitType.SURGEON || skillEffectRemain <= 0) {
            return;
        }
        double ang0 = skillEffectRemain * 4.2;
        java.awt.Stroke old = g.getStroke();
        g.setStroke(new java.awt.BasicStroke(2.4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 6; i++) {
            double a = ang0 + i * Math.PI / 3.0;
            double cx = x + Math.cos(a) * 30;
            double cy = y + Math.sin(a) * 30;
            double tx = Math.cos(a);
            double ty = Math.sin(a);
            int x1 = (int) (cx - tx * 11);
            int y1 = (int) (cy - ty * 11);
            int x2 = (int) (cx + tx * 11);
            int y2 = (int) (cy + ty * 11);
            g.setColor(new Color(230, 240, 255));
            g.drawLine(x1, y1, x2, y2);
            g.setColor(new Color(200, 60, 80));
            g.fillOval(x2 - 3, y2 - 3, 6, 6);
        }
        g.setStroke(old);
    }

    private void drawSkillPip(Graphics2D g, double top) {
        int w = 28;
        int x0 = (int) (x - w / 2.0);
        int y0 = (int) top;
        g.setColor(new Color(20, 20, 28, 200));
        g.fillRect(x0, y0, w, 3);
        if (isSkillReady()) {
            RenderUtils.glow(g, x, top + 1, 12, new Color(255, 210, 80, (int) (80 + 70 * VisualClock.pulse(7, 0))));
        }
        g.setColor(isSkillReady() ? new Color(255, 220, 80) : new Color(90, 180, 255));
        g.fillRect(x0, y0, (int) Math.max(1, w * getSkillChargeRatio()), 3);
    }

    protected abstract void drawBody(Graphics2D g);
}

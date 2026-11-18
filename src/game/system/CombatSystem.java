package game.system;

import game.Config;
import game.audio.Audio;
import game.enemies.Enemy;
import game.enemies.HIV;
import game.units.Unit;
import game.units.UnitType;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * 战斗系统：目标选择、治疗、光环、技能群体攻击。
 */
public class CombatSystem {

    public static final class AttackResult {
        public final List<Projectile> projectiles = new ArrayList<>();
        public final List<VisualEffect> effects = new ArrayList<>();
        public final List<FloatingText> texts = new ArrayList<>();
    }

    /**
     * 每帧重置倍率，再叠商店、信息科相邻加成、HIV 削弱与技能强化。
     */
    public void applyAuras(List<Unit> units, List<Enemy> enemies, ShopSystem shop) {
        for (Unit unit : units) {
            unit.resetMultipliers();
            unit.applyAttackMul(1.0 + shop.attackBonus());
            unit.applyHealMul(1.0 + shop.healBonus());
        }
        for (Unit info : units) {
            if (!info.isAlive() || info.getType() != UnitType.INFO) {
                continue;
            }
            for (Unit ally : units) {
                if (ally.isAlive() && ally != info && info.isAdjacentTo(ally)) {
                    ally.applyAttackMul(Config.INFO_ADJACENT_BONUS);
                    ally.applyHealMul(Config.INFO_ADJACENT_BONUS);
                }
            }
        }
        double aura = HIV.AURA_TILES * Config.TILE_SIZE;
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || enemy.getType() != game.enemies.EnemyType.HIV) {
                continue;
            }
            for (Unit unit : units) {
                if (unit.isAlive() && unit.distanceTo(enemy.getX(), enemy.getY()) <= aura) {
                    unit.applyAttackMul(HIV.ATTACK_FACTOR);
                }
            }
        }
        for (Unit unit : units) {
            if (unit.getType() == UnitType.ORTHOPEDIC || unit.getType() == UnitType.RADIOLOGY) {
                if (unit.isAoeActive() || (unit.getType() == UnitType.ORTHOPEDIC && unit.extraRangeTiles() > 0)) {
                    unit.applyAttackMul(Config.SKILL_ATTACK_BONUS);
                }
            }
        }
    }

    public void updateBlocking(List<Unit> units, List<Enemy> enemies) {
        double radius = Config.BLOCK_RADIUS * Config.TILE_SIZE;
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            Unit current = enemy.getBlocker();
            if (current != null && current.isAlive() && enemy.distanceTo(current.getX(), current.getY()) <= radius * 1.15) {
                continue;
            }
            if (current != null) {
                current.unblock(enemy);
                enemy.setBlocker(null);
            }
            Unit best = null;
            double bestDist = radius;
            for (Unit unit : units) {
                if (!unit.canBlock() || !unit.hasBlockCapacity()) {
                    continue;
                }
                double d = enemy.distanceTo(unit.getX(), unit.getY());
                if (d <= bestDist) {
                    bestDist = d;
                    best = unit;
                }
            }
            if (best != null) {
                best.block(enemy);
                enemy.setBlocker(best);
            }
        }
    }

    public AttackResult processAttacks(List<Unit> units, List<Enemy> enemies) {
        AttackResult result = new AttackResult();
        for (Unit unit : units) {
            if (!unit.isAlive() || unit.getType().isSupport()) {
                continue;
            }
            if (unit.getType().isHealer()) {
                processHeal(unit, units, result);
                continue;
            }
            if (unit.isAoeActive()) {
                if (!unit.tryAttackReady()) {
                    continue;
                }
                strikeAll(unit, enemies, result);
                continue;
            }
            Enemy target = selectTarget(unit, enemies);
            if (target == null || !unit.tryAttackReady()) {
                continue;
            }
            if (unit.getType().isRanged()) {
                boolean slow = unit.getType() == UnitType.ANESTHESIA;
                result.projectiles.add(new Projectile(
                        unit.getX(), unit.getY(), target,
                        unit.getAttackDamage(), slow, unit.getType().getAccent()));
                result.effects.add(new VisualEffect(
                        VisualEffect.Kind.BEAM,
                        unit.getX(), unit.getY(), target.getX(), target.getY(),
                        unit.getType().getAccent(), 0.12));
            } else {
                hit(unit, target, result);
                Audio.play(Audio.Cue.ATTACK);
            }
        }
        return result;
    }

    private void strikeAll(Unit unit, List<Enemy> enemies, AttackResult result) {
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive()) {
                continue;
            }
            hit(unit, enemy, result);
            result.effects.add(new VisualEffect(
                    VisualEffect.Kind.SKY_LASER,
                    enemy.getX(), enemy.getY(), enemy.getX(), -36,
                    new Color(255, 240, 140), 0.28));
        }
    }

    private void hit(Unit unit, Enemy target, AttackResult result) {
        double dmg = unit.getAttackDamage();
        target.takeDamage(dmg);
        result.effects.add(new VisualEffect(
                VisualEffect.Kind.SLASH,
                target.getX(), target.getY(),
                unit.getType().getAccent(), 0.18));
        result.texts.add(new FloatingText(
                String.valueOf((int) dmg),
                target.getX(), target.getY() - 16,
                new Color(255, 230, 180)));
    }

    private void processHeal(Unit nurse, List<Unit> units, AttackResult result) {
        if (!nurse.tryAttackReady()) {
            return;
        }
        boolean healed = false;
        for (Unit ally : units) {
            if (!ally.isAlive() || ally == nurse) {
                continue;
            }
            if (nurse.distanceTo(ally.getX(), ally.getY()) <= nurse.getRangePixels()) {
                if (ally.getHpRatio() < 0.999) {
                    double amount = nurse.getHealAmount();
                    ally.heal(amount);
                    ally.addHealGlow(0.55);
                    healed = true;
                    result.effects.add(new VisualEffect(
                            VisualEffect.Kind.HEAL, ally.getX(), ally.getY(),
                            new Color(80, 220, 140), 0.35));
                    result.texts.add(new FloatingText(
                            "+" + (int) amount, ally.getX(), ally.getY() - 18,
                            new Color(120, 255, 170)));
                }
            }
        }
        if (healed) {
            result.effects.add(new VisualEffect(
                    VisualEffect.Kind.RING, nurse.getX(), nurse.getY(),
                    new Color(255, 140, 170), 0.30));
        }
    }

    public Enemy selectTarget(Unit unit, List<Enemy> enemies) {
        Enemy blockedBest = null;
        double blockedProg = -1;
        for (Enemy e : unit.getBlockedEnemies()) {
            if (e.isAlive() && e.getPathProgress() > blockedProg) {
                blockedProg = e.getPathProgress();
                blockedBest = e;
            }
        }
        if (blockedBest != null) {
            return blockedBest;
        }
        Enemy best = null;
        double bestProg = -1;
        for (Enemy e : enemies) {
            if (!e.isAlive()) {
                continue;
            }
            if (!unit.inRange(e.getX(), e.getY())) {
                continue;
            }
            if (e.getPathProgress() > bestProg) {
                bestProg = e.getPathProgress();
                best = e;
            }
        }
        return best;
    }
}

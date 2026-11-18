package game;

import game.audio.Audio;
import game.enemies.Enemy;
import game.enemies.EnemyType;
import game.map.GameMap;
import game.map.Tile;
import game.system.CombatSystem;
import game.system.FloatingText;
import game.system.Projectile;
import game.system.ResourceManager;
import game.system.ShopItem;
import game.system.ShopSystem;
import game.system.VisualEffect;
import game.system.WaveManager;
import game.ui.SkillAnnounce;
import game.ui.SkillToast;
import game.units.Unit;
import game.units.UnitType;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 对局世界：地图、单位、敌人、技能、商店与胜负判定。
 */
public class GameWorld {

    private final GameMap map;
    private final ResourceManager resources = new ResourceManager();
    private final WaveManager waves = new WaveManager();
    private final CombatSystem combat = new CombatSystem();
    private final ShopSystem shop = new ShopSystem();

    private final List<Unit> units = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<VisualEffect> effects = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final Map<UnitType, Double> redeploy = new EnumMap<>(UnitType.class);

    private GameState state = GameState.PREPARING;
    private GameState stateBeforePause = GameState.PREPARING;
    private int lives = Config.PLAYER_LIVES;
    private UnitType selectedType;
    private volatile boolean retreatMode;
    private int hoverCol = -1;
    private int hoverRow = -1;
    private final IntroCamera intro;
    private boolean matchStarted;
    private String banner = "";
    private double bannerTimer;
    private boolean fastForward;
    private double globalSlowRemain;
    private double globalVulnRemain;
    private double binaryWipeAge = -1;
    private boolean binaryWipeKilled;
    private String shopMessage = "";
    private double shopMessageTimer;
    private SkillAnnounce skillAnnounce;
    private final List<SkillToast> skillToasts = new ArrayList<>();
    private double shakeRemain;

    public GameWorld(long seed) {
        this.map = new GameMap(seed);
        this.intro = new IntroCamera(map.getPath());
        for (UnitType type : UnitType.values()) {
            redeploy.put(type, 0.0);
        }
    }

    public synchronized void update(double dt) {
        if (!intro.isFinished()) {
            intro.update(dt);
            if (intro.isFinished()) {
                beginMatch();
            }
            return;
        }
        updateBinaryWipe(dt);
        updatePresentation(dt);
        if (state == GameState.SHOP) {
            if (shopMessageTimer > 0) {
                shopMessageTimer -= dt;
            }
            updateVfx(dt);
            return;
        }
        if (state == GameState.PAUSED || state == GameState.VICTORY || state == GameState.DEFEAT) {
            updateVfx(dt);
            return;
        }

        resources.update(dt);
        tickRedeploy(dt);
        tickGlobalEffects(dt);

        boolean preparing = state == GameState.PREPARING;
        List<EnemyType> toSpawn = waves.update(dt, preparing);
        if (preparing && waves.isPrepareFinished()) {
            state = GameState.COMBAT;
            setBanner("第 " + waves.getDisplayWaveNumber() + " 波来袭！");
            toSpawn = waves.update(0, false);
        }
        for (EnemyType type : toSpawn) {
            enemies.add(Enemy.create(type, map.getPath()));
        }

        boolean canCharge = state == GameState.PREPARING || state == GameState.COMBAT;
        for (Unit unit : units) {
            unit.update(dt, canCharge);
            if (unit.pollSkillJustReady()) {
                skillToasts.add(0, new SkillToast(unit.getType().getDisplayName() + " 技能就绪"));
                if (skillToasts.size() > 4) {
                    skillToasts.remove(skillToasts.size() - 1);
                }
            }
        }
        combat.applyAuras(units, enemies, shop);
        refreshEnemyStatuses();
        for (Enemy enemy : enemies) {
            enemy.update(dt);
        }
        combat.updateBlocking(units, enemies);

        CombatSystem.AttackResult attack = combat.processAttacks(units, enemies);
        projectiles.addAll(attack.projectiles);
        effects.addAll(attack.effects);
        floatingTexts.addAll(attack.texts);

        for (Projectile p : projectiles) {
            p.update(dt);
            if (p.pollImpact()) {
                Audio.play(Audio.Cue.ATTACK);
                effects.add(new VisualEffect(VisualEffect.Kind.BURST, p.getImpactX(), p.getImpactY(), p.getColor(), 0.22));
            }
        }
        updateVfx(dt);
        resolveDeathsAndLeaks();
        removeDeadUnits();
        checkWaveClear();
        checkEnd();
        decayBanner(dt);
    }

    private void updatePresentation(double dt) {
        if (skillAnnounce != null) {
            skillAnnounce.update(dt);
            if (skillAnnounce.isDead()) {
                skillAnnounce = null;
            }
        }
        for (SkillToast toast : skillToasts) {
            toast.update(dt);
        }
        skillToasts.removeIf(SkillToast::isDead);
        if (shakeRemain > 0) {
            shakeRemain = Math.max(0, shakeRemain - dt);
        }
    }

    private void tickGlobalEffects(double dt) {
        if (globalSlowRemain > 0) {
            globalSlowRemain = Math.max(0, globalSlowRemain - dt);
        }
        if (globalVulnRemain > 0) {
            globalVulnRemain = Math.max(0, globalVulnRemain - dt);
        }
    }

    private void refreshEnemyStatuses() {
        double vuln = shop.shredBonus() + (globalVulnRemain > 0 ? Config.SURGEON_VULN : 0);
        for (Enemy enemy : enemies) {
            enemy.setExtraVuln(vuln);
            if (globalSlowRemain > 0) {
                enemy.applySlow(globalSlowRemain);
                enemy.showDebuffMark(globalSlowRemain);
            }
            if (globalVulnRemain > 0) {
                enemy.showDebuffMark(globalVulnRemain);
            }
        }
    }

    private void tickRedeploy(double dt) {
        for (UnitType type : UnitType.values()) {
            double remain = redeploy.get(type);
            if (remain > 0) {
                redeploy.put(type, Math.max(0, remain - dt));
            }
        }
    }

    private void updateBinaryWipe(double dt) {
        if (binaryWipeAge < 0) {
            return;
        }
        binaryWipeAge += dt;
        if (!binaryWipeKilled && binaryWipeAge >= Config.BINARY_COVER) {
            wipeAllEnemies();
            binaryWipeKilled = true;
            setBanner("系统清除");
        }
        if (binaryWipeAge >= Config.BINARY_TOTAL) {
            binaryWipeAge = -1;
        }
    }

    private void updateVfx(double dt) {
        for (VisualEffect e : effects) {
            e.update(dt);
        }
        for (FloatingText t : floatingTexts) {
            t.update(dt);
        }
        effects.removeIf(VisualEffect::isDead);
        floatingTexts.removeIf(FloatingText::isDead);
        projectiles.removeIf(Projectile::isDead);
    }

    private void resolveDeathsAndLeaks() {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy enemy = it.next();
            if (enemy.hasLeaked()) {
                lives -= enemy.getType().getLeakDamage();
                effects.add(new VisualEffect(VisualEffect.Kind.BURST,
                        enemy.getX(), enemy.getY(), new Color(230, 60, 70), 0.35));
                setBanner("有病毒突入终点！-" + enemy.getType().getLeakDamage() + " 生命");
                it.remove();
                continue;
            }
            if (!enemy.isAlive()) {
                Audio.play(Audio.Cue.KILL);
                grantKillReward(enemy);
                it.remove();
            }
        }
        if (lives < 0) {
            lives = 0;
        }
    }

    private void grantKillReward(Enemy enemy) {
        int reward = enemy.getType().getReward();
        int gold = enemy.getType().getGold();
        resources.add(reward);
        resources.addGold(gold);
        floatingTexts.add(new FloatingText("+" + gold + "金", enemy.getX(), enemy.getY(), new Color(255, 220, 90)));
        effects.add(new VisualEffect(VisualEffect.Kind.BURST,
                enemy.getX(), enemy.getY(), enemy.getType().getFill(), 0.28));
    }

    private void removeDeadUnits() {
        Iterator<Unit> it = units.iterator();
        while (it.hasNext()) {
            Unit unit = it.next();
            if (!unit.isAlive()) {
                Tile tile = map.getTile(unit.getCol(), unit.getRow());
                if (tile != null) {
                    tile.setOccupied(false);
                }
                redeploy.put(unit.getType(), unit.getType().getRedeploySeconds());
                effects.add(new VisualEffect(VisualEffect.Kind.BURST,
                        unit.getX(), unit.getY(), unit.getType().getAccent(), 0.4));
                it.remove();
            }
        }
    }

    private void checkWaveClear() {
        if (state != GameState.COMBAT) {
            return;
        }
        if (waves.isCurrentWaveSpawnFinished() && enemies.isEmpty()) {
            waves.completeCurrentWave();
            if (waves.isAllWavesCleared()) {
                state = GameState.VICTORY;
                setBanner("全部波次已拦截");
            } else {
                state = GameState.SHOP;
                Audio.play(Audio.Cue.SHOP);
                shopMessage = "选择永久增益，或继续下一波";
                shopMessageTimer = 3.0;
                setBanner("波次结束，进入补给商店");
            }
        }
    }

    private void checkEnd() {
        if (lives <= 0 && state != GameState.VICTORY) {
            lives = 0;
            state = GameState.DEFEAT;
            setBanner("防线失守");
        }
    }

    private void decayBanner(double dt) {
        if (bannerTimer > 0) {
            bannerTimer -= dt;
        }
    }

    public void setBanner(String text) {
        this.banner = text;
        this.bannerTimer = 2.8;
    }

    private void beginMatch() {
        if (matchStarted) {
            return;
        }
        matchStarted = true;
        setBanner("第 1 波即将开始，请部署医生");
    }

    public synchronized void skipIntro() {
        if (intro.isFinished()) {
            return;
        }
        intro.finish();
        beginMatch();
    }

    public boolean isIntroPlaying() {
        return !intro.isFinished();
    }

    public IntroCamera getIntroCamera() {
        return intro;
    }

    public double getViewZoom() {
        return intro.getZoom();
    }

    public double getViewCamX() {
        return intro.getCamX();
    }

    public double getViewCamY() {
        return intro.getCamY();
    }

    public synchronized boolean tryDeploy() {
        if (isIntroPlaying()) {
            return false;
        }
        if (selectedType == null || state == GameState.VICTORY || state == GameState.DEFEAT || state == GameState.SHOP) {
            return false;
        }
        if (!map.canDeploy(selectedType, hoverCol, hoverRow)) {
            setBanner("无法部署在此处");
            return false;
        }
        if (redeploy.get(selectedType) > 0.05) {
            setBanner(selectedType.getDisplayName() + " 冷却中");
            return false;
        }
        if (!resources.trySpend(selectedType.getCost())) {
            setBanner("医疗资源不足");
            return false;
        }
        double x = map.centerX(hoverCol);
        double y = map.centerY(hoverRow);
        Unit unit = Unit.create(selectedType, hoverCol, hoverRow, x, y);
        units.add(unit);
        map.getTile(hoverCol, hoverRow).setOccupied(true);
        redeploy.put(selectedType, selectedType.getRedeploySeconds());
        effects.add(new VisualEffect(VisualEffect.Kind.RING, x, y, selectedType.getFill(), 0.35));
        Audio.play(Audio.Cue.DEPLOY);
        selectedType = null;
        return true;
    }

    public synchronized boolean tryCastSkillAtHover() {
        if (isIntroPlaying()) {
            return false;
        }
        Unit target = unitAt(hoverCol, hoverRow);
        if (target == null) {
            return false;
        }
        if (!target.isSkillReady()) {
            setBanner(target.getType().getDisplayName() + " 技能充能中");
            return true;
        }
        return castSkill(target);
    }

    public synchronized boolean tryCastSelectedSkill() {
        if (isIntroPlaying()) {
            return false;
        }
        if (selectedType == null) {
            return false;
        }
        for (Unit unit : units) {
            if (unit.getType() == selectedType && unit.isSkillReady()) {
                return castSkill(unit);
            }
        }
        setBanner("没有就绪的技能");
        return false;
    }

    private boolean castSkill(Unit unit) {
        unit.consumeSkill();
        UnitType type = unit.getType();
        skillAnnounce = new SkillAnnounce(type);
        switch (type) {
            case RADIOLOGY:
                fireSkyLasers();
                setBanner(type.getDisplayName() + " 技能发动");
                break;
            case ORTHOPEDIC:
                shakeRemain = Config.SCREEN_SHAKE;
                setBanner(type.getDisplayName() + " 技能发动");
                break;
            case PROTECTIVE:
                setBanner(type.getDisplayName() + " 技能发动");
                break;
            case NURSE:
                for (Unit ally : units) {
                    if (ally.isAlive()) {
                        ally.heal(ally.getMaxHp() * Config.NURSE_SKILL_HEAL);
                        ally.addHealGlow(1.25);
                        ally.showBuffMark(1.2);
                        effects.add(new VisualEffect(VisualEffect.Kind.HEAL,
                                ally.getX(), ally.getY(), new Color(80, 220, 140), 0.4));
                    }
                }
                setBanner("全体治疗");
                break;
            case ANESTHESIA:
                globalSlowRemain = type.getSkillDuration();
                for (Enemy enemy : enemies) {
                    enemy.applySlow(globalSlowRemain);
                    enemy.showDebuffMark(globalSlowRemain);
                }
                setBanner("全场麻醉减速");
                break;
            case SURGEON:
                globalVulnRemain = type.getSkillDuration();
                for (Enemy enemy : enemies) {
                    enemy.showDebuffMark(globalVulnRemain);
                }
                setBanner("手术破防");
                break;
            case INFO:
                Audio.play(Audio.Cue.INFO);
                startBinaryWipe();
                setBanner("数据覆盖中");
                break;
            default:
                break;
        }
        return true;
    }

    private void fireSkyLasers() {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                effects.add(new VisualEffect(
                        VisualEffect.Kind.SKY_LASER,
                        enemy.getX(), enemy.getY(), enemy.getX(), -36,
                        new Color(255, 240, 140), 0.32));
            }
        }
    }

    private void wipeAllEnemies() {
        List<Enemy> snapshot = new ArrayList<>(enemies);
        for (Enemy enemy : snapshot) {
            if (enemy.isAlive()) {
                enemy.takeDamage(enemy.getHp() + 99999);
            }
        }
    }

    private void startBinaryWipe() {
        binaryWipeAge = 0;
        binaryWipeKilled = false;
    }

    public double getBinaryWipeAge() {
        return binaryWipeAge;
    }

    public synchronized boolean tryShopClick(int mx, int my) {
        if (state != GameState.SHOP) {
            return false;
        }
        Rectangle cont = ShopSystem.continueRect();
        if (cont.contains(mx, my)) {
            closeShop();
            return true;
        }
        ShopItem[] items = ShopItem.values();
        for (int i = 0; i < items.length; i++) {
            if (ShopSystem.itemRect(i).contains(mx, my)) {
                buyItem(items[i]);
                return true;
            }
        }
        return false;
    }

    private void buyItem(ShopItem item) {
        if (shop.owns(item)) {
            shopMessage = "已拥有 " + item.getDisplayName();
            shopMessageTimer = 2.0;
            return;
        }
        if (shop.tryBuy(item, resources)) {
            shopMessage = "购买成功：" + item.getDisplayName();
        } else {
            shopMessage = "金币不足";
        }
        shopMessageTimer = 2.0;
    }

    public synchronized void closeShop() {
        if (state != GameState.SHOP) {
            return;
        }
        state = GameState.PREPARING;
        waves.beginNextPrepare();
        setBanner("第 " + waves.getDisplayWaveNumber() + " 波准备中");
    }

    public synchronized boolean tryRetreatAtHover() {
        if (isIntroPlaying() || state == GameState.SHOP) {
            return false;
        }
        Unit target = unitAt(hoverCol, hoverRow);
        if (target == null) {
            selectedType = null;
            return false;
        }
        int refund = (int) Math.floor(target.getType().getCost() * Config.RETREAT_REFUND_RATE);
        resources.add(refund);
        Tile tile = map.getTile(target.getCol(), target.getRow());
        if (tile != null) {
            tile.setOccupied(false);
        }
        redeploy.put(target.getType(), target.getType().getRedeploySeconds());
        for (Enemy blocked : new ArrayList<>(target.getBlockedEnemies())) {
            blocked.setBlocker(null);
            target.unblock(blocked);
        }
        units.remove(target);
        floatingTexts.add(new FloatingText("+" + refund, target.getX(), target.getY(), new Color(180, 220, 255)));
        effects.add(new VisualEffect(VisualEffect.Kind.RING, target.getX(), target.getY(),
                new Color(180, 220, 255), 0.3));
        retreatMode = false;
        setBanner("撤回 " + target.getType().getDisplayName() + "，返还 " + refund + " 部署点");
        return true;
    }

    public Unit unitAt(int col, int row) {
        for (Unit unit : units) {
            if (unit.getCol() == col && unit.getRow() == row) {
                return unit;
            }
        }
        return null;
    }

    public synchronized void togglePause() {
        if (isIntroPlaying() || state == GameState.VICTORY || state == GameState.DEFEAT || state == GameState.SHOP) {
            return;
        }
        if (state == GameState.PAUSED) {
            state = stateBeforePause;
        } else {
            stateBeforePause = state;
            state = GameState.PAUSED;
        }
    }

    public synchronized void toggleFastForward() {
        if (isIntroPlaying()) {
            return;
        }
        fastForward = !fastForward;
    }

    public boolean isFastForward() {
        return fastForward;
    }

    public void selectType(UnitType type) {
        if (isIntroPlaying()) {
            return;
        }
        retreatMode = false;
        selectedType = type;
    }

    public void clearSelection() {
        selectedType = null;
        retreatMode = false;
    }

    public synchronized void toggleRetreatMode() {
        if (isIntroPlaying() || state == GameState.SHOP
                || state == GameState.VICTORY || state == GameState.DEFEAT) {
            return;
        }
        retreatMode = !retreatMode;
        if (retreatMode) {
            selectedType = null;
            setBanner("点击场上的医生，取消部署并返还一半部署点");
        } else {
            setBanner("已退出取消部署");
        }
    }

    public boolean isRetreatMode() {
        return retreatMode;
    }

    public void setHoverTile(int col, int row) {
        hoverCol = col;
        hoverRow = row;
    }

    public GameMap getMap() {
        return map;
    }

    public GameState getState() {
        return state;
    }

    public int getLives() {
        return lives;
    }

    public ResourceManager getResources() {
        return resources;
    }

    public WaveManager getWaves() {
        return waves;
    }

    public ShopSystem getShop() {
        return shop;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    public List<VisualEffect> getEffects() {
        return effects;
    }

    public List<FloatingText> getFloatingTexts() {
        return floatingTexts;
    }

    public UnitType getSelectedType() {
        return selectedType;
    }

    public int getHoverCol() {
        return hoverCol;
    }

    public int getHoverRow() {
        return hoverRow;
    }

    public double getRedeploy(UnitType type) {
        return redeploy.get(type);
    }

    public String getBanner() {
        return banner;
    }

    public double getBannerTimer() {
        return bannerTimer;
    }

    public String getShopMessage() {
        return shopMessage;
    }

    public double getShopMessageTimer() {
        return shopMessageTimer;
    }

    public SkillAnnounce getSkillAnnounce() {
        return skillAnnounce;
    }

    public List<SkillToast> getSkillToasts() {
        return skillToasts;
    }

    public double getShakeX() {
        if (shakeRemain <= 0) {
            return 0;
        }
        double mag = 8.5 * (shakeRemain / Config.SCREEN_SHAKE);
        return Math.sin(shakeRemain * 91.7) * mag;
    }

    public double getShakeY() {
        if (shakeRemain <= 0) {
            return 0;
        }
        double mag = 6.0 * (shakeRemain / Config.SCREEN_SHAKE);
        return Math.cos(shakeRemain * 73.3) * mag;
    }
}

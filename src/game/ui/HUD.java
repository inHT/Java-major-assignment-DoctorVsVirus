package game.ui;

import game.Config;
import game.GameState;
import game.GameWorld;
import game.IntroCamera;
import game.map.GameMap;
import game.map.Tile;
import game.map.TileType;
import game.system.FloatingText;
import game.system.Projectile;
import game.system.ShopItem;
import game.system.ShopSystem;
import game.system.VisualEffect;
import game.units.UnitType;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

/**
 * 顶部状态栏、底部部署栏、商店、射程预览与提示条。
 */
public class HUD {

    public void draw(Graphics2D g, GameWorld world) {
        if (world.isIntroPlaying()) {
            drawIntroChrome(g, world);
            drawBackButton(g);
            return;
        }
        drawTopBar(g, world);
        drawBottomBar(g, world);
        drawBanner(g, world);
        drawHints(g);
        if (world.getState() == GameState.SHOP) {
            drawShop(g, world);
        }
        drawSkillToasts(g, world);
        drawSkillAnnounce(g, world);
        drawBackButton(g);
    }

    private void drawSkillAnnounce(Graphics2D g, GameWorld world) {
        if (world.getSkillAnnounce() != null) {
            world.getSkillAnnounce().draw(g);
        }
    }

    private void drawSkillToasts(Graphics2D g, GameWorld world) {
        java.util.List<SkillToast> toasts = world.getSkillToasts();
        for (int i = 0; i < toasts.size(); i++) {
            toasts.get(i).draw(g, i);
        }
    }

    public static Rectangle backButtonRect() {
        return new Rectangle(16, 12, 108, 32);
    }

    private void drawIntroChrome(Graphics2D g, GameWorld world) {
        IntroCamera intro = world.getIntroCamera();
        RenderUtils.fillRound(g, 10, 8, Config.WINDOW_WIDTH - 20, 40, 10, new Color(12, 22, 36, 230));
        g.setFont(Fonts.UI_BOLD);
        g.setColor(new Color(140, 210, 255));
        RenderUtils.drawCenteredString(g, "路线预览", Config.WINDOW_WIDTH / 2, 28);
        g.setFont(Fonts.SMALL);
        g.setColor(new Color(180, 200, 220));
        g.drawString("种子 " + world.getMap().getSeed(), 678, 34);

        int y = Config.TOP_BAR + Config.MAP_PIXEL_HEIGHT + 6;
        int h = Config.BOTTOM_BAR - 14;
        RenderUtils.fillRound(g, 8, y, Config.WINDOW_WIDTH - 16, h, 12, new Color(12, 22, 36, 235));

        float alpha = Math.max(0.15f, intro.getCaptionAlpha());
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setFont(Fonts.TITLE);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, intro.getCaption(), Config.WINDOW_WIDTH / 2, y + 48);
        g.setFont(Fonts.UI);
        g.setColor(new Color(190, 210, 225));
        RenderUtils.drawCenteredString(g, intro.getSubcaption(), Config.WINDOW_WIDTH / 2, y + 82);
        g.setComposite(old);

        g.setFont(Fonts.SMALL);
        g.setColor(new Color(150, 170, 190));
        RenderUtils.drawCenteredString(g, "空格跳过", Config.WINDOW_WIDTH / 2, y + h - 28);
    }

    public void drawBackButton(Graphics2D g) {
        Rectangle r = backButtonRect();
        boolean hover = UiCursor.contains(r.x, r.y, r.width, r.height);
        if (hover) {
            RenderUtils.glow(g, r.x + r.width / 2.0, r.y + r.height / 2.0, 36, new Color(230, 120, 140, 90));
        }
        RenderUtils.fillRound(g, r.x, r.y, r.width, r.height, 8, hover ? new Color(110, 55, 75, 240) : new Color(70, 40, 55, 235));
        RenderUtils.drawRound(g, r.x, r.y, r.width, r.height, 8, new Color(230, 160, 170), hover ? 2f : 1.4f);
        g.setFont(Fonts.SMALL);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, "返回主界面", r.x + r.width / 2, r.y + r.height / 2);
    }

    public static Rectangle cancelDeployRect() {
        int w = 132;
        int h = 46;
        int x = Config.WINDOW_WIDTH - 18 - w;
        int cardsTop = Config.TOP_BAR + Config.MAP_PIXEL_HEIGHT + 10;
        int y = cardsTop - h - 10;
        return new Rectangle(x, y, w, h);
    }

    public static Rectangle cardRect(int index) {
        int n = UnitType.values().length;
        int y = Config.TOP_BAR + Config.MAP_PIXEL_HEIGHT + 10;
        int totalW = Config.WINDOW_WIDTH - 24;
        int gap = 4;
        int w = (totalW - gap * (n - 1)) / n;
        int x = 12 + index * (w + gap);
        return new Rectangle(x, y, w, Config.BOTTOM_BAR - 26);
    }

    public void drawDeployPreview(Graphics2D g, GameWorld world, int ox, int oy) {
        if (world.isRetreatMode()) {
            drawRetreatMark(g, world, ox, oy);
            return;
        }
        UnitType selected = world.getSelectedType();
        int col = world.getHoverCol();
        int row = world.getHoverRow();
        GameMap map = world.getMap();
        if (selected == null || !map.inBounds(col, row) || world.getState() == GameState.SHOP) {
            return;
        }
        int x = ox + col * Config.TILE_SIZE;
        int y = oy + row * Config.TILE_SIZE;
        boolean ok = map.canDeploy(selected, col, row)
                && world.getRedeploy(selected) <= 0.05
                && world.getResources().canAfford(selected.getCost());
        Color c = ok ? new Color(80, 220, 140, 90) : new Color(230, 70, 70, 90);
        g.setColor(c);
        g.fillRect(x, y, Config.TILE_SIZE, Config.TILE_SIZE);
        g.setColor(ok ? new Color(80, 255, 160) : new Color(255, 120, 120));
        float dash = (float) ((VisualClock.seconds() * 18) % 10);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] {5f, 4f}, dash));
        g.drawRect(x + 2, y + 2, Config.TILE_SIZE - 4, Config.TILE_SIZE - 4);

        double cx = ox + map.centerX(col);
        double cy = oy + map.centerY(row);
        Sprites.drawUnit(g, selected, cx, cy, Sprites.PREVIEW_HEIGHT, ok ? 0.85f : 0.45f);
        double r = selected.getRangeTiles() * Config.TILE_SIZE;
        g.setColor(RenderUtils.withAlpha(selected.getAccent(), 36));
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g.setColor(RenderUtils.withAlpha(selected.getAccent(), 170));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[] {8f, 7f}, dash));
        g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    public void drawFieldPointer(Graphics2D g, GameWorld world, int ox, int oy) {
        if (world.getState() == GameState.SHOP) {
            return;
        }
        game.units.Unit unit = world.unitAt(world.getHoverCol(), world.getHoverRow());
        if (unit == null || !unit.isAlive()) {
            return;
        }
        double cx = ox + unit.getX();
        double cy = oy + unit.getY();
        if (world.getSelectedType() == null) {
            double pulse = 20 + 4 * VisualClock.pulse(6, 0);
            g.setStroke(new BasicStroke(1.8f));
            g.setColor(RenderUtils.withAlpha(unit.getType().getAccent(), 180));
            g.drawOval((int) (cx - pulse), (int) (cy - pulse), (int) (pulse * 2), (int) (pulse * 2));
        }
        if (unit.isSkillReady()) {
            boolean portrait = Sprites.hasPortrait(unit.getType());
            drawSkillReadyMark(g, cx, cy - (portrait ? 64 : 52));
        }
    }

    private void drawSkillReadyMark(Graphics2D g, double cx, double top) {
        int w = 76;
        int h = 20;
        int x = (int) Math.round(cx - w / 2.0);
        int y = (int) Math.round(top);
        RenderUtils.glow(g, cx, y + h / 2.0, 24, new Color(255, 196, 60, 100));
        RenderUtils.fillRound(g, x, y, w, h, 10, new Color(42, 30, 8, 230));
        RenderUtils.drawRound(g, x, y, w, h, 10, new Color(255, 214, 90), 1.5f);
        g.setColor(new Color(255, 220, 90));
        int ix = x + 8;
        int iy = y + 5;
        g.fillPolygon(new int[] {ix + 4, ix, ix + 3, ix + 1, ix + 8, ix + 5, ix + 8},
                new int[] {iy, iy + 5, iy + 5, iy + 10, iy + 4, iy + 4, iy}, 7);
        g.setFont(Fonts.TINY);
        g.setColor(new Color(255, 236, 170));
        RenderUtils.drawCenteredString(g, "技能就绪", x + 46, y + h / 2);
    }

    public void drawProjectiles(Graphics2D g, GameWorld world, int ox, int oy) {
        for (Projectile p : world.getProjectiles()) {
            int n = p.getTrailSize();
            for (int i = n - 1; i >= 0; i--) {
                float a = (n - i) / (float) (n + 1);
                RenderUtils.glow(g, ox + p.getTrailX(i), oy + p.getTrailY(i), 7,
                        RenderUtils.withAlpha(p.getColor(), (int) (40 + 50 * a)));
            }
            RenderUtils.glow(g, ox + p.getX(), oy + p.getY(), 12, RenderUtils.withAlpha(p.getColor(), 110));
            RenderUtils.fillCircle(g, ox + p.getX(), oy + p.getY(), 3.4, p.getColor(), Color.WHITE);
        }
    }

    public void drawEffects(Graphics2D g, GameWorld world, int ox, int oy) {
        for (VisualEffect e : world.getEffects()) {
            float alpha = (float) (1.0 - e.progress());
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, alpha)));
            int x = (int) (ox + e.x);
            int y = (int) (oy + e.y);
            g.setColor(e.color);
            switch (e.kind) {
                case SLASH:
                    g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(x - 12, y + 9, x + 14, y - 12);
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(x - 8, y + 4, x + 10, y - 8);
                    g.drawLine(x - 4, y + 12, x + 16, y - 4);
                    break;
                case HEAL:
                    RenderUtils.glow(g, x, y, 16 + 10 * e.progress(), RenderUtils.withAlpha(e.color, 120));
                    RenderUtils.drawMedicalCross(g, x, y, 12 + 8 * e.progress(), e.color);
                    g.setColor(Color.WHITE);
                    g.fillOval(x - 2, (int) (y - 8 - 10 * e.progress()), 3, 3);
                    break;
                case BEAM:
                    g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(x, y, (int) (ox + e.x2), (int) (oy + e.y2));
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(x, y, (int) (ox + e.x2), (int) (oy + e.y2));
                    break;
                case BURST:
                    int s = (int) (8 + 26 * e.progress());
                    RenderUtils.glow(g, x, y, s + 6, RenderUtils.withAlpha(e.color, 130));
                    g.setStroke(new BasicStroke(2f));
                    g.setColor(e.color);
                    g.drawOval(x - s, y - s, s * 2, s * 2);
                    g.setColor(Color.WHITE);
                    g.drawOval(x - s / 2, y - s / 2, s, s);
                    break;
                case RING:
                    int rs = (int) (6 + 24 * e.progress());
                    g.setStroke(new BasicStroke(2.4f));
                    g.drawOval(x - rs, y - rs, rs * 2, rs * 2);
                    g.setStroke(new BasicStroke(1.1f));
                    g.setColor(Color.WHITE);
                    g.drawOval(x - rs + 4, y - rs + 4, rs * 2 - 8, rs * 2 - 8);
                    break;
                case SKY_LASER:
                    double drop = Math.min(1.0, e.progress() / 0.35);
                    int x1 = (int) (ox + e.x2);
                    int y1 = (int) (oy + e.y2);
                    int x2 = x;
                    int y2 = (int) (y1 + (y - y1) * drop);
                    g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(new Color(255, 230, 80, 90));
                    g.drawLine(x1, y1, x2, y2);
                    g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.setColor(Color.WHITE);
                    g.drawLine(x1, y1, x2, y2);
                    if (drop > 0.92) {
                        g.setColor(new Color(255, 250, 180, 180));
                        g.fillOval(x - 8, y - 8, 16, 16);
                    }
                    break;
                default:
                    break;
            }
            g.setComposite(old);
        }
        g.setFont(Fonts.SMALL);
        for (FloatingText t : world.getFloatingTexts()) {
            float pop = t.age < 0.12 ? (float) (0.72 + 0.48 * (t.age / 0.12)) : 1f;
            Graphics2D tg = (Graphics2D) g.create();
            tg.translate(ox + t.x, oy + t.y);
            tg.scale(pop, pop);
            tg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, t.alpha()));
            tg.setColor(t.color);
            tg.setFont(Fonts.SMALL);
            RenderUtils.drawCenteredString(tg, t.text, 0, 0);
            tg.dispose();
        }
    }

    private void drawTopBar(Graphics2D g, GameWorld world) {
        RenderUtils.fillRound(g, 10, 8, Config.WINDOW_WIDTH - 20, 40, 10, new Color(12, 22, 36, 230));
        RenderUtils.sheen(g, 10, 8, Config.WINDOW_WIDTH - 20, 40);
        int scan = 140 + (int) ((VisualClock.seconds() * 90) % (Config.WINDOW_WIDTH - 180));
        g.setColor(new Color(140, 210, 255, 28));
        g.fillRect(scan, 12, 36, 32);
        g.setFont(Fonts.UI_BOLD);
        g.setColor(new Color(255, 110, 120));
        g.drawString("生命 " + world.getLives(), 136, 34);
        g.setColor(new Color(120, 210, 255));
        g.drawString("资源 " + world.getResources().getDp(), 232, 34);
        g.setColor(new Color(255, 214, 80));
        g.drawString("金币 " + world.getResources().getGold(), 344, 34);
        g.setColor(new Color(140, 210, 255));
        String wave = "波次 " + world.getWaves().getDisplayWaveNumber() + "/" + world.getWaves().getTotalWaves();
        g.drawString(wave, 454, 34);

        GameState state = world.getState();
        String phase;
        Color phaseColor;
        if (state == GameState.PREPARING) {
            phase = String.format("准备 %.0f 秒", world.getWaves().getPrepareRemain());
            phaseColor = new Color(120, 230, 170);
        } else if (state == GameState.COMBAT) {
            phase = "进攻中";
            phaseColor = new Color(255, 150, 90);
        } else if (state == GameState.SHOP) {
            phase = "补给商店";
            phaseColor = new Color(255, 210, 120);
        } else if (state == GameState.PAUSED) {
            phase = "已暂停";
            phaseColor = new Color(220, 220, 120);
        } else if (state == GameState.VICTORY) {
            phase = "胜利";
            phaseColor = new Color(120, 255, 170);
        } else {
            phase = "失败";
            phaseColor = new Color(255, 90, 90);
        }
        g.setColor(phaseColor);
        g.drawString(phase, 574, 34);

        g.setColor(new Color(180, 200, 220));
        g.setFont(Fonts.SMALL);
        g.drawString("种子 " + world.getMap().getSeed(), 678, 34);
        if (world.isFastForward()) {
            g.setColor(new Color(255, 200, 80));
            g.drawString("×2", 788, 34);
        }
        g.setColor(new Color(160, 180, 200));
        g.drawString("病毒 " + world.getEnemies().size(), 828, 34);
    }

    private void drawBottomBar(Graphics2D g, GameWorld world) {
        int y = Config.TOP_BAR + Config.MAP_PIXEL_HEIGHT + 6;
        RenderUtils.fillRound(g, 8, y, Config.WINDOW_WIDTH - 16, Config.BOTTOM_BAR - 14, 12, new Color(12, 22, 36, 235));
        UnitType[] types = UnitType.values();
        int hoverIndex = -1;
        for (int i = 0; i < types.length; i++) {
            Rectangle r = cardRect(i);
            if (UiCursor.contains(r.x, r.y, r.width, r.height)) {
                hoverIndex = i;
            }
        }
        for (int i = 0; i < types.length; i++) {
            if (i == hoverIndex) {
                continue;
            }
            Rectangle r = cardRect(i);
            drawCard(g, world, types[i], r.x, r.y, r.width, r.height, false);
        }
        if (hoverIndex >= 0) {
            Rectangle r = cardRect(hoverIndex);
            Graphics2D cg = (Graphics2D) g.create();
            double cx = r.x + r.width / 2.0;
            double cy = r.y + r.height / 2.0;
            cg.translate(cx, cy);
            cg.scale(1.1, 1.1);
            cg.translate(-cx, -cy);
            drawCard(cg, world, types[hoverIndex], r.x, r.y, r.width, r.height, true);
            cg.dispose();
        }
        drawCancelButton(g, world);
    }

    private void drawCancelButton(Graphics2D g, GameWorld world) {
        Rectangle r = cancelDeployRect();
        boolean hover = UiCursor.contains(r.x, r.y, r.width, r.height);
        boolean active = world.isRetreatMode();
        Color fill = active ? new Color(150, 64, 74) : new Color(46, 62, 86);
        if (hover) {
            fill = new Color(
                    Math.min(255, fill.getRed() + 28),
                    Math.min(255, fill.getGreen() + 24),
                    Math.min(255, fill.getBlue() + 24));
            RenderUtils.glow(g, r.x + r.width / 2.0, r.y + r.height / 2.0, 36, new Color(255, 150, 150, active ? 110 : 70));
        }
        RenderUtils.fillRound(g, r.x, r.y, r.width, r.height, 10, fill);
        RenderUtils.sheen(g, r.x, r.y, r.width, r.height);
        Color edge = active ? new Color(255, 180, 170) : new Color(170, 200, 220);
        RenderUtils.drawRound(g, r.x, r.y, r.width, r.height, 10, edge, active ? 2.2f : 1.4f);
        g.setFont(Fonts.SMALL);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, "取消部署", r.x + r.width / 2, r.y + 16);
        g.setFont(Fonts.TINY);
        g.setColor(active ? new Color(255, 220, 200) : new Color(180, 200, 215));
        RenderUtils.drawCenteredString(g, "返还50%", r.x + r.width / 2, r.y + 34);
    }

    private void drawRetreatMark(Graphics2D g, GameWorld world, int ox, int oy) {
        int col = world.getHoverCol();
        int row = world.getHoverRow();
        if (!world.getMap().inBounds(col, row) || world.unitAt(col, row) == null) {
            return;
        }
        int x = ox + col * Config.TILE_SIZE;
        int y = oy + row * Config.TILE_SIZE;
        g.setColor(new Color(220, 70, 80, 90));
        g.fillRect(x, y, Config.TILE_SIZE, Config.TILE_SIZE);
        g.setColor(new Color(255, 150, 150));
        g.setStroke(new BasicStroke(2f));
        g.drawRect(x + 2, y + 2, Config.TILE_SIZE - 4, Config.TILE_SIZE - 4);
    }

    private void drawCard(Graphics2D g, GameWorld world, UnitType type, int x, int y, int w, int h, boolean hover) {
        boolean selected = world.getSelectedType() == type;
        boolean afford = world.getResources().canAfford(type.getCost());
        double charge = 0;
        for (game.units.Unit unit : world.getUnits()) {
            if (unit.getType() == type) {
                charge = Math.max(charge, unit.getSkillChargeRatio());
            }
        }
        Color bg;
        Color border;
        Color nameColor;
        Color metaColor;
        if (!afford) {
            bg = new Color(28, 28, 32);
            border = new Color(90, 90, 96);
            nameColor = new Color(150, 150, 155);
            metaColor = new Color(120, 120, 125);
        } else if (selected) {
            bg = new Color(40, 70, 90);
            border = type.getAccent();
            nameColor = Color.WHITE;
            metaColor = new Color(170, 190, 210);
        } else {
            bg = new Color(22, 36, 52);
            border = hover ? new Color(150, 210, 235) : new Color(70, 90, 110);
            nameColor = Color.WHITE;
            metaColor = new Color(170, 190, 210);
        }
        if (selected && afford) {
            RenderUtils.glow(g, x + w / 2.0, y + h / 2.0, w * 0.62, RenderUtils.withAlpha(type.getAccent(), 80));
        } else if (hover) {
            RenderUtils.glow(g, x + w / 2.0, y + 20, 28, new Color(160, 220, 255, 70));
        }
        RenderUtils.fillRound(g, x, y, w, h, 10, bg);
        if (afford) {
            RenderUtils.sheen(g, x, y, w, h);
        }
        float borderW = selected && afford ? 2.4f : 1.2f;
        if (charge > 0.99 && afford) {
            border = new Color(255, 214, 90);
            borderW = 1.4f + 1.2f * VisualClock.pulse(6.5, 0);
        }
        RenderUtils.drawRound(g, x, y, w, h, 10, border, borderW);

        float portraitAlpha = afford ? 1f : 0.38f;
        boolean portrait = Sprites.drawUnit(g, type, x + w / 2.0, y + 28, Sprites.CARD_HEIGHT, portraitAlpha);
        if (!portrait) {
            Color fill = afford ? type.getFill() : new Color(90, 90, 95);
            Color accent = afford ? type.getAccent() : new Color(70, 70, 74);
            RenderUtils.fillCircle(g, x + w / 2.0, y + 22, 12, fill, accent);
        }
        g.setFont(Fonts.TINY);
        g.setColor(nameColor);
        RenderUtils.drawCenteredString(g, type.getDisplayName(), x + w / 2, y + 50);
        g.setColor(metaColor);
        RenderUtils.drawCenteredString(g, type.getHotkey() + " · " + type.getCost(), x + w / 2, y + 64);

        int barW = w - 12;
        int barX = x + 6;
        int barY = y + h - 18;
        g.setColor(new Color(20, 24, 32));
        g.fillRoundRect(barX, barY, barW, 8, 4, 4);
        Color chargeColor = afford ? new Color(80, 160, 230) : new Color(90, 90, 96);
        if (charge > 0.99 && afford) {
            chargeColor = new Color(255, 206, 70);
        }
        g.setColor(chargeColor);
        g.fillRoundRect(barX, barY, Math.max(2, (int) (barW * charge)), 8, 4, 4);

        g.setFont(Fonts.TINY);
        g.setColor(afford ? new Color(150, 200, 230) : new Color(120, 120, 125));
        RenderUtils.drawCenteredString(g, type.getSkillDesc(), x + w / 2, y + h - 28);

        double cd = world.getRedeploy(type);
        if (cd > 0.05) {
            g.setColor(new Color(8, 12, 20, 160));
            g.fillRoundRect(x, y, w, h, 10, 10);
            g.setFont(Fonts.UI_BOLD);
            g.setColor(Color.WHITE);
            RenderUtils.drawCenteredString(g, String.format("%.0f", Math.ceil(cd)), x + w / 2, y + h / 2);
        }
    }

    private void drawShop(Graphics2D g, GameWorld world) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT);
        int px = ShopSystem.panelX();
        int py = ShopSystem.panelY();
        RenderUtils.fillRound(g, px, py, ShopSystem.panelW(), ShopSystem.panelH(), 18, new Color(16, 28, 44, 245));
        RenderUtils.drawRound(g, px, py, ShopSystem.panelW(), ShopSystem.panelH(), 18, new Color(120, 190, 230), 2f);

        g.setFont(Fonts.TITLE);
        g.setColor(new Color(255, 230, 140));
        RenderUtils.drawCenteredString(g, "补给商店", Config.WINDOW_WIDTH / 2, py + 28);
        g.setFont(Fonts.UI_BOLD);
        g.setColor(new Color(255, 214, 80));
        RenderUtils.drawCenteredString(g, "金币 " + world.getResources().getGold(), Config.WINDOW_WIDTH / 2, py + 56);
        g.setFont(Fonts.TINY);
        g.setColor(new Color(180, 200, 220));
        RenderUtils.drawCenteredString(g, "可直接购买任意等级，不需要先买初级", Config.WINDOW_WIDTH / 2, py + 74);

        ShopItem[] items = ShopItem.values();
        for (int i = 0; i < items.length; i++) {
            drawShopItem(g, world, items[i], ShopSystem.itemRect(i));
        }

        Rectangle cont = ShopSystem.continueRect();
        boolean contHover = UiCursor.contains(cont.x, cont.y, cont.width, cont.height);
        if (contHover) {
            RenderUtils.glow(g, cont.x + cont.width / 2.0, cont.y + cont.height / 2.0, 70, new Color(80, 190, 220, 90));
        }
        RenderUtils.fillRound(g, cont.x, cont.y, cont.width, cont.height, 10,
                contHover ? new Color(55, 165, 195) : new Color(40, 140, 170));
        RenderUtils.sheen(g, cont.x, cont.y, cont.width, cont.height);
        g.setFont(Fonts.UI_BOLD);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, "下一波", cont.x + cont.width / 2, cont.y + cont.height / 2);

        if (world.getShopMessageTimer() > 0 && world.getShopMessage() != null) {
            g.setFont(Fonts.SMALL);
            g.setColor(new Color(255, 230, 180));
            RenderUtils.drawCenteredString(g, world.getShopMessage(), Config.WINDOW_WIDTH / 2, cont.y - 14);
        }
    }

    private void drawShopItem(Graphics2D g, GameWorld world, ShopItem item, Rectangle r) {
        boolean owned = world.getShop().owns(item);
        boolean afford = world.getResources().getGold() >= item.getPrice();
        boolean hover = UiCursor.contains(r.x, r.y, r.width, r.height);
        Color bg = owned ? new Color(40, 70, 50) : (hover ? new Color(32, 52, 74) : new Color(24, 38, 56));
        if (hover) {
            RenderUtils.glow(g, r.x + r.width / 2.0, r.y + r.height / 2.0, r.width * 0.45, new Color(120, 190, 230, 60));
        }
        RenderUtils.fillRound(g, r.x, r.y, r.width, r.height, 10, bg);
        RenderUtils.sheen(g, r.x, r.y, r.width, 28);
        RenderUtils.drawRound(g, r.x, r.y, r.width, r.height, 10,
                owned ? new Color(120, 200, 140) : (hover ? new Color(160, 220, 255) : new Color(80, 120, 160)),
                hover ? 2f : 1.4f);
        g.setFont(Fonts.UI_BOLD);
        g.setColor(Color.WHITE);
        g.drawString(item.getDisplayName(), r.x + 12, r.y + 24);
        g.setFont(Fonts.TINY);
        g.setColor(new Color(180, 200, 220));
        g.drawString(item.getDesc(), r.x + 12, r.y + 46);
        if (owned) {
            g.setColor(new Color(140, 230, 160));
            g.drawString("已拥有", r.x + 12, r.y + 74);
        } else {
            g.setColor(afford ? new Color(255, 214, 80) : new Color(255, 110, 110));
            g.drawString("价格 " + item.getPrice(), r.x + 12, r.y + 74);
        }
    }

    private void drawBanner(Graphics2D g, GameWorld world) {
        if (world.getBannerTimer() <= 0 || world.getState() == GameState.SHOP) {
            return;
        }
        float a = (float) Math.min(1.0, world.getBannerTimer());
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        int w = 420;
        int x = (Config.WINDOW_WIDTH - w) / 2;
        int y = Config.TOP_BAR + 12;
        RenderUtils.fillRound(g, x, y, w, 32, 8, new Color(10, 16, 28, 210));
        g.setColor(new Color(120, 210, 255, 200));
        g.fillRoundRect(x + 8, y + 8, 4, 16, 2, 2);
        g.setFont(Fonts.UI_BOLD);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, world.getBanner(), Config.WINDOW_WIDTH / 2, y + 16);
        g.setComposite(old);
    }

    private void drawHints(Graphics2D g) {
        g.setFont(Fonts.TINY);
        g.setColor(new Color(140, 160, 180));
        String hint = "1-7选择  左键部署/放技能  右下角取消部署  右键也可撤回  E技能  空格暂停  F倍速";
        g.drawString(hint, 16, Config.WINDOW_HEIGHT - 6);
    }

    public boolean isDeployableHighlight(Tile tile) {
        return tile != null && (tile.getType() == TileType.PATH || tile.getType() == TileType.HIGH);
    }
}

package game;

import game.audio.Audio;
import game.ui.BinaryWipeEffect;
import game.ui.Fonts;
import game.ui.RenderUtils;
import game.ui.UiCursor;
import game.ui.VisualClock;
import game.units.UnitType;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;

import javax.swing.JPanel;

/**
 * 游戏渲染面板：负责绘制世界并转发输入。游戏逻辑由 GameWorld 处理。
 */
public class GamePanel extends JPanel {

    private final GameFrame frame;
    private final GameWorld world;
    private final GameLoop loop;
    private final InputHandler input;
    private final game.ui.HUD hud = new game.ui.HUD();
    private final Object lock = new Object();

    public GamePanel(GameFrame frame, long seed) {
        this.frame = frame;
        this.world = new GameWorld(seed);
        this.loop = new GameLoop(this);
        this.input = new InputHandler(world);
        setPreferredSize(new Dimension(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT));
        setBackground(new Color(8, 14, 24));
        setFocusable(true);
        bindInput();
    }

    public void start() {
        loop.start();
        requestFocusInWindow();
    }

    public void stop() {
        loop.stop();
    }

    /**
     * 由游戏循环线程调用。
     */
    public void tick(double dt) {
        synchronized (lock) {
            VisualClock.advance(dt);
            double scale = world.isFastForward() ? Config.TIME_SCALE_FAST : 1.0;
            world.update(dt * scale);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        RenderUtils.enableQuality(g);
        synchronized (lock) {
            drawWorld(g);
        }
        g.dispose();
    }

    private void drawWorld(Graphics2D g) {
        g.setColor(new Color(8, 14, 24));
        g.fillRect(0, 0, getWidth(), getHeight());

        int vx = Config.SIDE_PAD;
        int vy = Config.TOP_BAR;
        int vw = Config.MAP_PIXEL_WIDTH;
        int vh = Config.MAP_PIXEL_HEIGHT;
        double zoom = world.getViewZoom();
        double camX = world.getViewCamX();
        double camY = world.getViewCamY();

        Graphics2D mapG = (Graphics2D) g.create();
        // 先在屏幕坐标裁切地图区域，后续缩放不会把画面画进顶栏和底栏。
        mapG.setClip(vx, vy, vw, vh);
        if (zoom == 1.0) {
            int ox = vx + (int) Math.round(world.getShakeX());
            int oy = vy + (int) Math.round(world.getShakeY());
            mapG.translate(ox, oy);
        } else {
            mapG.translate(vx + vw / 2.0 + world.getShakeX(), vy + vh / 2.0 + world.getShakeY());
            mapG.scale(zoom, zoom);
            mapG.translate(-camX, -camY);
        }
        world.getMap().draw(mapG);
        world.getIntroCamera().draw(mapG);
        for (game.units.Unit unit : world.getUnits()) {
            unit.draw(mapG);
        }
        for (game.enemies.Enemy enemy : world.getEnemies()) {
            enemy.draw(mapG);
        }
        hud.drawProjectiles(mapG, world, 0, 0);
        hud.drawEffects(mapG, world, 0, 0);
        hud.drawDeployPreview(mapG, world, 0, 0);
        hud.drawFieldPointer(mapG, world, 0, 0);
        mapG.dispose();

        drawIntroVignette(g, vx, vy, vw, vh, zoom);
        drawIntroMarker(g, zoom, camX, camY);
        hud.draw(g, world);
        BinaryWipeEffect.draw(g, getWidth(), getHeight(), world.getBinaryWipeAge());
        drawOverlay(g);
        hud.drawBackButton(g);
    }

    private void drawIntroVignette(Graphics2D g, int vx, int vy, int vw, int vh, double zoom) {
        if (!world.isIntroPlaying() || zoom <= 1.08) {
            return;
        }
        float strength = (float) Math.min(1.0, (zoom - 1.0) / 1.7);
        int alpha = (int) (150 * strength);
        RadialGradientPaint paint = new RadialGradientPaint(
                new Point2D.Float(vx + vw / 2f, vy + vh / 2f),
                vw * 0.62f,
                new float[] {0.4f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, alpha)});
        Paint old = g.getPaint();
        g.setPaint(paint);
        g.fillRect(vx, vy, vw, vh);
        g.setPaint(old);
    }

    private void drawIntroMarker(Graphics2D g, double zoom, double camX, double camY) {
        if (!world.isIntroPlaying()) {
            return;
        }
        IntroCamera intro = world.getIntroCamera();
        String text = intro.getMarkerText();
        if (text == null) {
            return;
        }
        float alpha = intro.getCaptionAlpha();
        if (alpha <= 0.05f) {
            return;
        }
        double sx = Config.SIDE_PAD + Config.MAP_PIXEL_WIDTH / 2.0 + (intro.getMarkerX() - camX) * zoom;
        double sy = Config.TOP_BAR + Config.MAP_PIXEL_HEIGHT / 2.0 + (intro.getMarkerY() - camY) * zoom;
        sy -= 74 + Math.sin(System.nanoTime() / 280_000_000.0) * 3;
        int w = 72;
        int h = 28;
        int x = (int) Math.round(sx - w / 2.0);
        int y = (int) Math.round(sy - h / 2.0);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        RenderUtils.fillRound(g, x, y, w, h, 14, new Color(8, 16, 28, 210));
        RenderUtils.drawRound(g, x, y, w, h, 14, intro.getMarkerColor(), 1.6f);
        g.setFont(Fonts.UI_BOLD);
        g.setColor(intro.getMarkerColor());
        RenderUtils.drawCenteredString(g, text, (int) Math.round(sx), (int) Math.round(sy));
        g.setComposite(old);
    }

    private void drawOverlay(Graphics2D g) {
        GameState state = world.getState();
        if (state != GameState.PAUSED && state != GameState.VICTORY && state != GameState.DEFEAT) {
            return;
        }
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(Fonts.HUGE);
        String title;
        Color color;
        String sub;
        if (state == GameState.PAUSED) {
            title = "已暂停";
            color = new Color(230, 230, 180);
            sub = "按空格继续";
        } else if (state == GameState.VICTORY) {
            title = "胜利";
            color = new Color(120, 255, 180);
            sub = "病毒被全部拦截  点击或按 Enter 返回菜单";
        } else {
            title = "防线失守";
            color = new Color(255, 110, 110);
            sub = "点击或按 Enter 返回菜单";
        }
        g.setColor(color);
        RenderUtils.drawCenteredString(g, title, getWidth() / 2, getHeight() / 2 - 20);
        g.setFont(Fonts.SUBTITLE);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, sub, getWidth() / 2, getHeight() / 2 + 28);
    }

    private void bindInput() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                UiCursor.move(e.getX(), e.getY());
                requestFocusInWindow();
                if (e.getButton() == MouseEvent.BUTTON1 && game.ui.HUD.backButtonRect().contains(e.getX(), e.getY())) {
                    Audio.play(Audio.Cue.CLICK);
                    frame.showMenu();
                    return;
                }
                if (world.isIntroPlaying()) {
                    return;
                }
                GameState state = world.getState();
                if (state == GameState.VICTORY || state == GameState.DEFEAT) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        Audio.play(Audio.Cue.CLICK);
                    }
                    frame.showMenu();
                    return;
                }
                if (state == GameState.SHOP) {
                    if (e.getButton() == MouseEvent.BUTTON1 && world.tryShopClick(e.getX(), e.getY())) {
                        Audio.play(Audio.Cue.CLICK);
                    }
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (game.ui.HUD.cancelDeployRect().contains(e.getX(), e.getY())) {
                        Audio.play(Audio.Cue.CLICK);
                        world.toggleRetreatMode();
                        return;
                    }
                    if (trySelectCard(e.getX(), e.getY())) {
                        Audio.play(Audio.Cue.CLICK);
                        return;
                    }
                    input.onLeftClick();
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    input.onRightClick();
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                UiCursor.move(e.getX(), e.getY());
                if (world.isIntroPlaying()) {
                    return;
                }
                input.onMouseMoved(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                UiCursor.move(e.getX(), e.getY());
                if (world.isIntroPlaying()) {
                    return;
                }
                input.onMouseMoved(e.getX(), e.getY());
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (world.isIntroPlaying()) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        world.skipIntro();
                    }
                    return;
                }
                if (world.getState() == GameState.VICTORY || world.getState() == GameState.DEFEAT) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        frame.showMenu();
                    }
                    return;
                }
                if (world.getState() == GameState.SHOP) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                        world.closeShop();
                    }
                    return;
                }
                input.onKeyPressed(e.getKeyCode());
            }
        });
    }

    private boolean trySelectCard(int mx, int my) {
        UnitType[] types = UnitType.values();
        for (int i = 0; i < types.length; i++) {
            if (game.ui.HUD.cardRect(i).contains(mx, my)) {
                world.selectType(types[i]);
                return true;
            }
        }
        return false;
    }
}

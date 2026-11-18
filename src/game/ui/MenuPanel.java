package game.ui;

import game.Config;
import game.GameFrame;
import game.audio.Audio;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

/**
 * 主菜单：开始游戏、玩法说明与退出。
 */
public class MenuPanel extends JPanel {

    private final GameFrame frame;
    private final JTextField seedField;
    private final int btnX;
    private final int btnW = 220;
    private final int btnH = 48;
    private final int startY = 250;
    private final int howToY = 314;
    private final int exitY = 378;
    private boolean showingHowTo;
    private Timer anim;
    private int hoverButton = -1;

    public MenuPanel(GameFrame frame) {
        this.frame = frame;
        this.btnX = (Config.WINDOW_WIDTH - btnW) / 2;
        setPreferredSize(new Dimension(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT));
        setLayout(null);
        setBackground(new Color(8, 16, 28));

        seedField = new JTextField(String.valueOf(Config.DEFAULT_SEED));
        seedField.setBounds(btnX, 198, btnW, 32);
        seedField.setFont(Fonts.UI);
        seedField.setForeground(new Color(230, 240, 255));
        seedField.setBackground(new Color(20, 36, 56));
        seedField.setCaretColor(Color.WHITE);
        seedField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        add(seedField);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (showingHowTo) {
                    if (closeHowToRect().contains(e.getX(), e.getY())) {
                        Audio.play(Audio.Cue.CLICK);
                        showingHowTo = false;
                        seedField.setVisible(true);
                        repaint();
                    } else if (!howToPanelRect().contains(e.getX(), e.getY())) {
                        showingHowTo = false;
                        seedField.setVisible(true);
                        repaint();
                    }
                    return;
                }
                if (hit(e.getX(), e.getY(), startY)) {
                    Audio.play(Audio.Cue.CLICK);
                    frame.startGame(parseSeed());
                } else if (hit(e.getX(), e.getY(), howToY)) {
                    Audio.play(Audio.Cue.CLICK);
                    showingHowTo = true;
                    seedField.setVisible(false);
                    repaint();
                } else if (hit(e.getX(), e.getY(), exitY)) {
                    Audio.play(Audio.Cue.CLICK);
                    Timer exitLater = new Timer(180, ev -> System.exit(0));
                    exitLater.setRepeats(false);
                    exitLater.start();
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hoverButton = hoverAt(e.getX(), e.getY());
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        anim = new Timer(33, e -> {
            VisualClock.advance(0.033);
            repaint();
        });
        anim.start();
    }

    @Override
    public void removeNotify() {
        if (anim != null) {
            anim.stop();
        }
        super.removeNotify();
    }

    private int hoverAt(int mx, int my) {
        if (showingHowTo) {
            return closeHowToRect().contains(mx, my) ? 3 : -1;
        }
        if (hit(mx, my, startY)) {
            return 0;
        }
        if (hit(mx, my, howToY)) {
            return 1;
        }
        if (hit(mx, my, exitY)) {
            return 2;
        }
        return -1;
    }

    private boolean hit(int mx, int my, int y) {
        return mx >= btnX && mx <= btnX + btnW && my >= y && my <= y + btnH;
    }

    private long parseSeed() {
        try {
            return Long.parseLong(seedField.getText().trim());
        } catch (NumberFormatException ex) {
            return Config.DEFAULT_SEED;
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        RenderUtils.enableQuality(g);

        g.setColor(new Color(8, 16, 28));
        g.fillRect(0, 0, getWidth(), getHeight());
        drawDecor(g);

        float glow = 0.45f + 0.55f * VisualClock.pulse(2.1, 0);
        g.setFont(Fonts.HUGE);
        g.setColor(new Color(90, 190, 255, (int) (40 + 80 * glow)));
        RenderUtils.drawCenteredString(g, Config.GAME_TITLE_CN, getWidth() / 2, 88);
        g.setColor(new Color(186, 240, 255));
        RenderUtils.drawCenteredString(g, Config.GAME_TITLE_CN, getWidth() / 2, 86);
        g.setFont(Fonts.SUBTITLE);
        g.setColor(new Color(170, 205, 230));
        RenderUtils.drawCenteredString(g, Config.GAME_TITLE_EN, getWidth() / 2, 128);
        int line = 36 + (int) (18 * glow);
        g.setColor(new Color(120, 210, 255, 140));
        g.drawLine(getWidth() / 2 - line, 146, getWidth() / 2 + line, 146);

        if (!showingHowTo) {
            g.setFont(Fonts.SMALL);
            g.setColor(new Color(140, 170, 200));
            RenderUtils.drawCenteredString(g, "地图种子（相同种子生成相同地图）", getWidth() / 2, 184);
            RenderUtils.glow(g, btnX + btnW / 2.0, 214, 90, new Color(80, 170, 220, 70));

            drawButton(g, "开始游戏", startY, new Color(40, 140, 170), hoverButton == 0);
            drawButton(g, "玩法", howToY, new Color(50, 90, 140), hoverButton == 1);
            drawButton(g, "退出", exitY, new Color(120, 55, 70), hoverButton == 2);
        } else {
            drawHowTo(g);
        }
        g.dispose();
    }

    private void drawButton(Graphics2D g, String label, int y, Color fill, boolean hovered) {
        if (hovered) {
            RenderUtils.glow(g, btnX + btnW / 2.0, y + btnH / 2.0, 78, new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 120));
            fill = new Color(
                    Math.min(255, fill.getRed() + 32),
                    Math.min(255, fill.getGreen() + 32),
                    Math.min(255, fill.getBlue() + 28));
        }
        RenderUtils.fillRound(g, btnX, y, btnW, btnH, 12, fill);
        RenderUtils.sheen(g, btnX, y, btnW, btnH);
        int edge = hovered ? 230 : 170;
        RenderUtils.drawRound(g, btnX, y, btnW, btnH, 12, new Color(edge, 236, 255), hovered ? 2.4f : 1.5f);
        g.setFont(Fonts.TITLE);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, label, btnX + btnW / 2, y + btnH / 2);
    }

    private Rectangle howToPanelRect() {
        return new Rectangle(48, 48, Config.WINDOW_WIDTH - 96, Config.WINDOW_HEIGHT - 96);
    }

    private Rectangle closeHowToRect() {
        Rectangle p = howToPanelRect();
        return new Rectangle(p.x + p.width - 108, p.y + 16, 88, 32);
    }

    private void drawHowTo(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, getWidth(), getHeight());
        Rectangle p = howToPanelRect();
        RenderUtils.fillRound(g, p.x, p.y, p.width, p.height, 16, new Color(14, 26, 42, 245));
        RenderUtils.drawRound(g, p.x, p.y, p.width, p.height, 16, new Color(120, 190, 230), 2f);

        g.setFont(Fonts.TITLE);
        g.setColor(new Color(140, 210, 255));
        g.drawString("玩法", p.x + 28, p.y + 40);

        Rectangle close = closeHowToRect();
        Color closeFill = hoverButton == 3 ? new Color(70, 120, 180) : new Color(50, 90, 140);
        RenderUtils.fillRound(g, close.x, close.y, close.width, close.height, 8, closeFill);
        g.setFont(Fonts.UI_BOLD);
        g.setColor(Color.WHITE);
        RenderUtils.drawCenteredString(g, "关闭", close.x + close.width / 2, close.y + close.height / 2);

        g.setFont(Fonts.UI);
        g.setColor(new Color(210, 220, 230));
        String[] lines = {
                "【核心】病毒沿固定路径冲击终点。漏过会扣除生命，生命耗尽则防线失守；守住全部波次即胜利。",
                "击杀敌人获得部署资源与金币。资源用于放置医生，金币用于波次间商店购买永久增益。",
                "近战医生部署在路径上阻挡敌人，远程医生部署在高台射击。护士与信息科两者皆可。",
                "技能随时间充能，就绪后点击场上单位或按 E 释放。",
                "右下角「取消部署」后点选医生可撤回，返还一半部署点。空格暂停，F 切换倍速。",
                "",
                "【医生】",
                "骨科医生：近战阻挡，技能强化自身并拉长攻击距离。",
                "放射科医生：高台远程输出，技能强化后对全体敌人发动打击。",
                "防护服医生：高耐久阻挡，技能期间进入无敌。",
                "护士：治疗周围友军，技能为全体回复生命。",
                "麻醉科医生：远程干扰，技能使全场敌人减速。",
                "外科医生：高频近战，技能削弱敌方防御。",
                "信息科医生：不参与攻防，强化相邻医生；技能清除场上全部敌人，充能较久。",
                "",
                "【商店】每波结束后可花费金币购买手术刀、绷带、麻醉针等永久加成，等级互不依赖。",
                "数字键 1-7 选择对应医生。左键部署，右键或「取消部署」撤回。"
        };
        int y = p.y + 78;
        for (String line : lines) {
            if (line.startsWith("【")) {
                g.setFont(Fonts.UI_BOLD);
                g.setColor(new Color(140, 210, 255));
            } else {
                g.setFont(Fonts.UI);
                g.setColor(new Color(210, 220, 230));
            }
            g.drawString(line, p.x + 28, y);
            y += 26;
        }
    }

    private void drawDecor(Graphics2D g) {
        g.setColor(new Color(80, 150, 190, 16));
        int shift = (int) (VisualClock.seconds() * 14) % 56;
        for (int x = -56 + shift; x < getWidth(); x += 56) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = shift; y < getHeight(); y += 56) {
            g.drawLine(0, y, getWidth(), y);
        }
        RenderUtils.glow(g, 90, 560, 180, new Color(40, 140, 130, 90));
        RenderUtils.glow(g, getWidth() - 40, 40, 200, new Color(150, 40, 60, 80));
        double t = VisualClock.seconds();
        for (int i = 0; i < 16; i++) {
            double px = (Math.sin(t * 0.35 + i * 1.7) * 0.5 + 0.5) * getWidth();
            double py = (getHeight() + 40 - ((t * 18 + i * 47) % (getHeight() + 80)));
            int alpha = 40 + (i % 5) * 18;
            g.setColor(i % 3 == 0 ? new Color(255, 120, 130, alpha) : new Color(140, 220, 255, alpha));
            int s = 2 + i % 3;
            g.fillOval((int) px, (int) py, s, s);
        }
        float cross = 0.65f + 0.35f * VisualClock.pulse(2.4, 1);
        RenderUtils.drawMedicalCross(g, getWidth() / 2.0 - 210, 90, 18 + 6 * cross, new Color(230, 80, 90, (int) (160 + 70 * cross)));
        RenderUtils.glow(g, 120, 168, 28 + 8 * VisualClock.pulse(2.2, 0), new Color(90, 210, 230, 110));
        RenderUtils.fillCircle(g, 120, 168, 10, new Color(90, 210, 230), new Color(20, 80, 100));
        RenderUtils.glow(g, getWidth() - 130, 198, 32, new Color(220, 70, 70, 100));
        RenderUtils.fillCircle(g, getWidth() - 130, 198, 12, new Color(220, 70, 70), new Color(90, 20, 20));
    }
}

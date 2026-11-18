package game;

import game.map.Path;
import game.ui.RenderUtils;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.List;

/**
 * 开局运镜：对准入侵起点，沿路径移到终点，再拉远到整张地图。
 * 结束后停在与未缩放地图一致的视角，正式对局从这一帧之后开始。
 */
public class IntroCamera {

    private enum Phase {
        HOLD_SPAWN,
        TRAVEL,
        HOLD_EXIT,
        PULL_BACK,
        HOLD_WIDE,
        DONE
    }

    private static final double HOLD_SPAWN = 0.95;
    private static final double TRAVEL = 3.15;
    private static final double HOLD_EXIT = 0.8;
    private static final double PULL_BACK = 1.55;
    private static final double HOLD_WIDE = 0.45;

    /** 落幅时的近景倍率，以及入场时略远一点的起始倍率。 */
    private static final double FOCUS_ZOOM = 2.85;
    private static final double ARRIVE_ZOOM = 2.2;

    private final Path path;
    private final double spawnX;
    private final double spawnY;
    private final double exitX;
    private final double exitY;
    private final double playCamX = Config.MAP_PIXEL_WIDTH / 2.0;
    private final double playCamY = Config.MAP_PIXEL_HEIGHT / 2.0;

    private Phase phase = Phase.HOLD_SPAWN;
    private double phaseTime;
    private double elapsed;
    private double zoom = ARRIVE_ZOOM;
    private double camX;
    private double camY;
    private double shownDistance;
    private String caption = "敌方入侵起点";
    private String subcaption = "病毒从这里进入病区";
    private volatile boolean finished;

    public IntroCamera(Path path) {
        this.path = path;
        double[] spawn = path.getSpawnPoint();
        double[] exit = path.getExitPoint();
        this.spawnX = spawn[0];
        this.spawnY = spawn[1];
        this.exitX = exit[0];
        this.exitY = exit[1];
        this.camX = spawnX;
        this.camY = spawnY;
        applyHoldSpawn();
    }

    public void update(double dt) {
        if (finished) {
            return;
        }
        elapsed += dt;
        phaseTime += dt;
        switch (phase) {
            case HOLD_SPAWN:
                applyHoldSpawn();
                if (phaseTime >= HOLD_SPAWN) {
                    enter(Phase.TRAVEL);
                }
                break;
            case TRAVEL:
                applyTravel();
                if (phaseTime >= TRAVEL) {
                    enter(Phase.HOLD_EXIT);
                }
                break;
            case HOLD_EXIT:
                applyHoldExit();
                if (phaseTime >= HOLD_EXIT) {
                    enter(Phase.PULL_BACK);
                }
                break;
            case PULL_BACK:
                applyPullBack();
                if (phaseTime >= PULL_BACK) {
                    enter(Phase.HOLD_WIDE);
                }
                break;
            case HOLD_WIDE:
                applyWide();
                if (phaseTime >= HOLD_WIDE) {
                    finish();
                }
                break;
            default:
                finish();
                break;
        }
    }

    public void finish() {
        applyPlayPose();
        phase = Phase.DONE;
        caption = "";
        subcaption = "";
        shownDistance = path.getTotalLength();
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public double getZoom() {
        return zoom;
    }

    public double getCamX() {
        return camX;
    }

    public double getCamY() {
        return camY;
    }

    public String getCaption() {
        return caption;
    }

    public String getSubcaption() {
        return subcaption;
    }

    public float getCaptionAlpha() {
        if (finished) {
            return 0f;
        }
        double dur = phaseDuration();
        double fade = 0.18;
        double alpha = 1;
        if (phaseTime < fade) {
            alpha = phaseTime / fade;
        }
        if (dur - phaseTime < fade) {
            alpha = Math.min(alpha, Math.max(0, (dur - phaseTime) / fade));
        }
        return (float) clamp01(alpha);
    }

    /**
     * 近景时标在起点或终点上方的短标签；拉远阶段不标。
     */
    public String getMarkerText() {
        if (phase == Phase.HOLD_SPAWN) {
            return "起点";
        }
        if (phase == Phase.HOLD_EXIT) {
            return "终点";
        }
        return null;
    }

    public double getMarkerX() {
        return phase == Phase.HOLD_EXIT ? exitX : spawnX;
    }

    public double getMarkerY() {
        return phase == Phase.HOLD_EXIT ? exitY : spawnY;
    }

    public Color getMarkerColor() {
        if (phase == Phase.HOLD_EXIT) {
            return new Color(255, 150, 160);
        }
        return new Color(140, 230, 255);
    }

    /**
     * 在地图坐标系中加亮已经展示过的路线，并在当前关注点画脉冲圈。
     */
    public void draw(Graphics2D g) {
        if (finished) {
            return;
        }
        drawRevealedPath(g);
        drawPulse(g);
    }

    private void enter(Phase next) {
        phase = next;
        phaseTime = 0;
        if (next == Phase.TRAVEL && path.getTotalLength() < 1) {
            enter(Phase.HOLD_EXIT);
            return;
        }
        switch (next) {
            case TRAVEL:
                applyTravel();
                break;
            case HOLD_EXIT:
                applyHoldExit();
                break;
            case PULL_BACK:
                applyPullBack();
                break;
            case HOLD_WIDE:
                applyWide();
                break;
            default:
                break;
        }
    }

    private void applyHoldSpawn() {
        double t = easeOut(phaseTime / HOLD_SPAWN);
        zoom = lerp(ARRIVE_ZOOM, FOCUS_ZOOM, t);
        camX = spawnX;
        camY = spawnY;
        shownDistance = 0;
        caption = "敌方入侵起点";
        subcaption = "病毒从这里进入病区";
    }

    private void applyTravel() {
        double t = mildEase(phaseTime / TRAVEL);
        double total = path.getTotalLength();
        double dist = t * total;
        shownDistance = dist;
        double wing = 46 * Math.sin(Math.PI * clamp01(t));
        double[] back = path.pointAtDistance(Math.max(0, dist - wing));
        double[] ahead = path.pointAtDistance(Math.min(total, dist + wing));
        camX = (back[0] + ahead[0]) * 0.5;
        camY = (back[1] + ahead[1]) * 0.5;
        zoom = FOCUS_ZOOM;
        caption = "沿路线推进";
        subcaption = "镜头正沿入侵路径移向终点";
    }

    private void applyHoldExit() {
        zoom = FOCUS_ZOOM;
        camX = exitX;
        camY = exitY;
        shownDistance = path.getTotalLength();
        caption = "防线终点";
        subcaption = "病毒抵达这里就会损失生命";
    }

    private void applyPullBack() {
        double t = easeInOut(phaseTime / PULL_BACK);
        zoom = lerp(FOCUS_ZOOM, 1.0, t);
        camX = lerp(exitX, playCamX, t);
        camY = lerp(exitY, playCamY, t);
        shownDistance = path.getTotalLength();
        caption = "";
        subcaption = "";
    }

    private void applyWide() {
        applyPlayPose();
        shownDistance = path.getTotalLength();
        caption = "";
        subcaption = "";
    }

    private void applyPlayPose() {
        zoom = 1.0;
        camX = playCamX;
        camY = playCamY;
    }

    private void drawRevealedPath(Graphics2D g) {
        float alpha = routeAlpha();
        if (alpha <= 0.02f || shownDistance <= 1) {
            return;
        }
        List<double[]> points = path.getWaypoints();
        if (points.size() < 2) {
            return;
        }
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(90, 210, 255));
        strokeUntil(g, points, shownDistance);
        g.setStroke(new BasicStroke(2.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(235, 250, 255));
        strokeUntil(g, points, shownDistance);
        if (phase == Phase.TRAVEL) {
            RenderUtils.fillCircle(g, camX, camY, 7, new Color(180, 245, 255), Color.WHITE);
        }
        g.setComposite(old);
    }

    private void strokeUntil(Graphics2D g, List<double[]> points, double distance) {
        double remain = distance;
        for (int i = 1; i < points.size() && remain > 0; i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            double seg = Math.hypot(b[0] - a[0], b[1] - a[1]);
            double t = seg <= 1e-6 ? 1 : Math.min(1, remain / seg);
            int x2 = (int) Math.round(a[0] + (b[0] - a[0]) * t);
            int y2 = (int) Math.round(a[1] + (b[1] - a[1]) * t);
            g.drawLine((int) Math.round(a[0]), (int) Math.round(a[1]), x2, y2);
            remain -= seg;
        }
    }

    private void drawPulse(Graphics2D g) {
        if (phase == Phase.HOLD_SPAWN || phase == Phase.TRAVEL) {
            float alpha = phase == Phase.HOLD_SPAWN ? 0.95f : (float) Math.max(0, 1 - phaseTime / 0.45);
            drawRing(g, spawnX, spawnY, new Color(120, 220, 255), alpha);
        }
        if (phase == Phase.HOLD_EXIT || phase == Phase.PULL_BACK || phase == Phase.TRAVEL) {
            float alpha;
            if (phase == Phase.TRAVEL) {
                alpha = (float) Math.max(0, (phaseTime - (TRAVEL - 0.45)) / 0.45);
            } else if (phase == Phase.HOLD_EXIT) {
                alpha = 0.95f;
            } else {
                alpha = (float) Math.max(0, 1 - phaseTime / 0.5);
            }
            drawRing(g, exitX, exitY, new Color(255, 120, 130), alpha);
        }
    }

    private void drawRing(Graphics2D g, double x, double y, Color color, float alpha) {
        if (alpha <= 0.04f) {
            return;
        }
        double pulse = 0.5 + 0.5 * Math.sin(elapsed * 4.6);
        double radius = 20 + 16 * pulse;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, alpha)));
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
        g.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
        g.setColor(color);
        g.setStroke(new BasicStroke(2.6f));
        g.drawOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
        g.setComposite(old);
    }

    private float routeAlpha() {
        if (phase == Phase.HOLD_SPAWN) {
            return 0f;
        }
        if (phase == Phase.PULL_BACK) {
            return (float) (1.0 - 0.55 * clamp01(phaseTime / PULL_BACK));
        }
        if (phase == Phase.HOLD_WIDE) {
            return (float) (0.45 * (1.0 - clamp01(phaseTime / HOLD_WIDE)));
        }
        return 1f;
    }

    private double phaseDuration() {
        switch (phase) {
            case HOLD_SPAWN:
                return HOLD_SPAWN;
            case TRAVEL:
                return TRAVEL;
            case HOLD_EXIT:
                return HOLD_EXIT;
            case PULL_BACK:
                return PULL_BACK;
            case HOLD_WIDE:
                return HOLD_WIDE;
            default:
                return 1;
        }
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * clamp01(t);
    }

    private static double clamp01(double t) {
        if (t < 0) {
            return 0;
        }
        if (t > 1) {
            return 1;
        }
        return t;
    }

    private static double easeOut(double t) {
        t = clamp01(t);
        double inv = 1 - t;
        return 1 - inv * inv * inv;
    }

    private static double easeInOut(double t) {
        t = clamp01(t);
        if (t < 0.5) {
            return 4 * t * t * t;
        }
        double inv = -2 * t + 2;
        return 1 - inv * inv * inv / 2;
    }

    /** 两端放慢、中段仍明显移动，避免拐弯处像卡住。 */
    private static double mildEase(double t) {
        t = clamp01(t);
        double s = t * t * (3 - 2 * t);
        return t * 0.4 + s * 0.6;
    }
}

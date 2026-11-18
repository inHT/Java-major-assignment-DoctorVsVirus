package game.ui;

/**
 * 仅供绘制使用的时间，不参与战斗结算。
 */
public final class VisualClock {

    private static double seconds;

    private VisualClock() {
    }

    public static void advance(double dt) {
        if (dt > 0) {
            seconds += dt;
        }
    }

    public static double seconds() {
        return seconds;
    }

    /** 0 到 1 的平滑脉冲。 */
    public static float pulse(double speed, double phase) {
        return (float) (0.5 + 0.5 * Math.sin(seconds * speed + phase));
    }
}

package game;

/**
 * 固定帧率游戏循环，运行在独立线程中。
 * 以 {@link Config#FPS} 为目标，使用纳秒计时并对过长帧做钳制。
 */
public class GameLoop implements Runnable {

    private final GamePanel panel;
    private volatile boolean running;
    private Thread thread;

    public GameLoop(GamePanel panel) {
        this.panel = panel;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this, "DoctorVsVirus-Loop");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        final long frameNanos = 1_000_000_000L / Config.FPS;
        long last = System.nanoTime();
        while (running) {
            long now = System.nanoTime();
            double dt = (now - last) / 1_000_000_000.0;
            last = now;
            if (dt > 0.08) {
                dt = 0.08;
            }
            try {
                panel.tick(dt);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            long elapsed = System.nanoTime() - now;
            long sleepNanos = frameNanos - elapsed;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}

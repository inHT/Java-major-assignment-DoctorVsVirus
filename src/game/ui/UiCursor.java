package game.ui;

/**
 * 最近一次鼠标位置，只用于悬停高亮，不改变点击判定。
 */
public final class UiCursor {

    private static int x = -1;
    private static int y = -1;

    private UiCursor() {
    }

    public static void move(int mx, int my) {
        x = mx;
        y = my;
    }

    public static boolean contains(int rx, int ry, int rw, int rh) {
        return x >= rx && y >= ry && x < rx + rw && y < ry + rh;
    }
}

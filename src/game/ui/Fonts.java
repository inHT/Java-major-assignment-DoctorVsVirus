package game.ui;

import java.awt.Font;

/**
 * 优先选用支持中文的系统字体，避免界面出现方块字。
 */
public final class Fonts {

    public static final Font HUGE = pick(40, Font.BOLD);
    public static final Font TITLE = pick(26, Font.BOLD);
    public static final Font SUBTITLE = pick(16, Font.PLAIN);
    public static final Font UI = pick(14, Font.PLAIN);
    public static final Font UI_BOLD = pick(14, Font.BOLD);
    public static final Font SMALL = pick(12, Font.PLAIN);
    public static final Font TINY = pick(11, Font.PLAIN);
    public static final Font MONO = mono(14);

    private Fonts() {
    }

    private static Font mono(int size) {
        String[] names = {"Consolas", "Cascadia Mono", "Courier New", "Monospaced"};
        for (String name : names) {
            Font font = new Font(name, Font.BOLD, size);
            if (font.canDisplay('0') && font.canDisplay('1')) {
                return font;
            }
        }
        return new Font("Monospaced", Font.BOLD, size);
    }

    private static Font pick(int size, int style) {
        String[] names = {
                "Microsoft YaHei UI",
                "Microsoft YaHei",
                "PingFang SC",
                "Noto Sans SC",
                "SimHei",
                "SansSerif"
        };
        for (String name : names) {
            Font font = new Font(name, style, size);
            if (font.canDisplay('医') && font.canDisplay('毒')) {
                return font;
            }
        }
        return new Font("SansSerif", style, size);
    }
}

package game;

import game.audio.Audio;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 程序入口。
 */
public class Main {

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        Audio.start();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 使用默认外观即可
            }
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}

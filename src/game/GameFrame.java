package game;

import game.ui.MenuPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * 主窗口：在菜单与对局面板之间切换。
 */
public class GameFrame extends JFrame {

    private MenuPanel menuPanel;
    private GamePanel gamePanel;

    public GameFrame() {
        super(Config.GAME_TITLE);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        showMenu();
        pack();
        setLocationRelativeTo(null);
    }

    public void showMenu() {
        if (gamePanel != null) {
            gamePanel.stop();
            gamePanel = null;
        }
        getContentPane().removeAll();
        menuPanel = new MenuPanel(this);
        add(menuPanel);
        revalidate();
        repaint();
        pack();
    }

    public void startGame(long seed) {
        if (gamePanel != null) {
            gamePanel.stop();
        }
        getContentPane().removeAll();
        gamePanel = new GamePanel(this, seed);
        add(gamePanel);
        revalidate();
        repaint();
        pack();
        SwingUtilities.invokeLater(() -> gamePanel.requestFocusInWindow());
        gamePanel.start();
    }
}

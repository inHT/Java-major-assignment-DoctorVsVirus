package game.ui;

import game.units.UnitType;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * 单位立绘加载与绘制。优先读取「游戏资产/角色立绘」。
 */
public final class Sprites {

    public static final int MAP_HEIGHT = 58;
    public static final int CARD_HEIGHT = 36;
    public static final int PREVIEW_HEIGHT = 54;

    private static final Map<UnitType, BufferedImage> PORTRAITS = new EnumMap<>(UnitType.class);

    static {
        for (UnitType type : UnitType.values()) {
            load(type, type.getDisplayName() + ".png");
        }
    }

    private Sprites() {
    }

    public static boolean hasPortrait(UnitType type) {
        return PORTRAITS.get(type) != null;
    }

    public static boolean drawUnit(Graphics2D g, UnitType type, double cx, double cy, int height) {
        return drawUnit(g, type, cx, cy, height, 1f);
    }

    public static boolean drawUnit(Graphics2D g, UnitType type, double cx, double cy, int height, float alpha) {
        BufferedImage image = PORTRAITS.get(type);
        if (image == null || height <= 0) {
            return false;
        }
        double scale = height / (double) image.getHeight();
        int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
        int h = height;
        int x = (int) Math.round(cx - w / 2.0);
        int y = (int) Math.round(cy - h / 2.0);
        Object oldInterp = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Composite old = g.getComposite();
        if (alpha < 0.999f) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, Math.min(1f, alpha))));
        }
        g.drawImage(image, x, y, w, h, null);
        g.setComposite(old);
        if (oldInterp != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterp);
        }
        return true;
    }

    private static void load(UnitType type, String fileName) {
        try (InputStream in = open(fileName)) {
            if (in == null) {
                System.err.println("未找到立绘: " + fileName);
                return;
            }
            BufferedImage raw = ImageIO.read(in);
            if (raw == null) {
                return;
            }
            PORTRAITS.put(type, crop(maybeKeyBlack(raw)));
        } catch (Exception ex) {
            System.err.println("加载立绘失败: " + fileName + " / " + ex.getMessage());
        }
    }

    private static InputStream open(String fileName) throws Exception {
        String[] resourcePaths = {
                "/游戏资产/角色立绘/" + fileName,
                "/game/assets/" + fileName
        };
        for (String res : resourcePaths) {
            InputStream in = Sprites.class.getResourceAsStream(res);
            if (in != null) {
                return in;
            }
        }
        String userDir = System.getProperty("user.dir");
        String[] candidates = {
                "游戏资产/角色立绘/" + fileName,
                "DoctorVsVirus/游戏资产/角色立绘/" + fileName,
                userDir + File.separator + "游戏资产" + File.separator + "角色立绘" + File.separator + fileName,
                userDir + File.separator + "DoctorVsVirus" + File.separator + "游戏资产" + File.separator + "角色立绘" + File.separator + fileName,
                "src/game/assets/" + fileName
        };
        for (String path : candidates) {
            File file = new File(path);
            if (file.isFile()) {
                return new FileInputStream(file);
            }
        }
        return null;
    }

    private static BufferedImage maybeKeyBlack(BufferedImage src) {
        int rgb = src.getRGB(0, 0);
        int a = (rgb >>> 24) & 0xFF;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (a > 200 && r < 18 && g < 18 && b < 18) {
            return keyBlack(src);
        }
        return src;
    }

    private static BufferedImage keyBlack(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int luma = (r + g + b) / 3;
                int alpha;
                if (r < 16 && g < 16 && b < 16) {
                    alpha = 0;
                } else if (r < 38 && g < 38 && b < 38) {
                    alpha = (int) ((luma - 16) / 22.0 * 255);
                    alpha = Math.max(0, Math.min(255, alpha));
                } else {
                    alpha = 255;
                }
                dst.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return dst;
    }

    private static BufferedImage crop(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int minX = w;
        int minY = h;
        int maxX = 0;
        int maxY = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (src.getRGB(x, y) >>> 24) & 0xFF;
                if (a > 12) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }
        }
        if (maxX <= minX || maxY <= minY) {
            return src;
        }
        int pad = 4;
        minX = Math.max(0, minX - pad);
        minY = Math.max(0, minY - pad);
        maxX = Math.min(w - 1, maxX + pad);
        maxY = Math.min(h - 1, maxY + pad);
        int cw = maxX - minX + 1;
        int ch = maxY - minY + 1;
        BufferedImage copy = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, cw, ch, minX, minY, maxX + 1, maxY + 1, null);
        g.dispose();
        return copy;
    }
}

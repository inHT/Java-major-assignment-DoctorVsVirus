package game.map;

import game.Config;
import game.ui.Fonts;
import game.ui.RenderUtils;
import game.ui.VisualClock;
import game.units.UnitType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 基于种子生成的固定地图。
 * 同一种子始终得到同一路径、高台与出生/终点位置。
 */
public class GameMap {

    private final int cols;
    private final int rows;
    private final long seed;
    private final Tile[][] tiles;
    private final Path path;

    public GameMap(long seed) {
        this.cols = Config.MAP_COLS;
        this.rows = Config.MAP_ROWS;
        this.seed = seed;
        this.tiles = new Tile[rows][cols];
        this.path = new Path();
        generate();
    }

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public long getSeed() {
        return seed;
    }

    public Path getPath() {
        return path;
    }

    public Tile getTile(int col, int row) {
        if (!inBounds(col, row)) {
            return null;
        }
        return tiles[row][col];
    }

    public Tile tileAtPixel(double x, double y) {
        int col = (int) (x / Config.TILE_SIZE);
        int row = (int) (y / Config.TILE_SIZE);
        return getTile(col, row);
    }

    public boolean inBounds(int col, int row) {
        return col >= 0 && row >= 0 && col < cols && row < rows;
    }

    public boolean canDeploy(UnitType type, int col, int row) {
        Tile tile = getTile(col, row);
        if (tile == null || tile.isOccupied()) {
            return false;
        }
        return type.canDeployOn(tile.getType());
    }

    public double centerX(int col) {
        return (col + 0.5) * Config.TILE_SIZE;
    }

    public double centerY(int row) {
        return (row + 0.5) * Config.TILE_SIZE;
    }

    /**
     * 生成带弯道的折线走廊，再把路径旁的墙改为高台。
     */
    private void generate() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tiles[r][c] = new Tile(c, r, TileType.WALL);
            }
        }

        Random rng = new Random(seed);
        int startY = 2 + rng.nextInt(rows - 4);
        int t1 = 3 + rng.nextInt(3);
        int t2 = 8 + rng.nextInt(3);
        int t3 = 13 + rng.nextInt(2);

        int y2 = randomLane(rng, startY);
        int y3 = randomLane(rng, y2);
        int endY = randomLane(rng, y3);

        addLine(0, startY, t1, startY);
        addLine(t1, startY, t1, y2);
        addLine(t1, y2, t2, y2);
        addLine(t2, y2, t2, y3);
        addLine(t2, y3, t3, y3);
        addLine(t3, y3, t3, endY);
        addLine(t3, endY, cols - 1, endY);

        List<Point> cells = path.getCells();
        Set<Point> pathSet = new HashSet<>(cells);
        for (int i = 0; i < cells.size(); i++) {
            Point p = cells.get(i);
            TileType type = TileType.PATH;
            if (i == 0) {
                type = TileType.SPAWN;
            } else if (i == cells.size() - 1) {
                type = TileType.EXIT;
            }
            tiles[p.y][p.x].setType(type);
        }

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (Point p : pathSet) {
            for (int[] d : dirs) {
                int nc = p.x + d[0];
                int nr = p.y + d[1];
                if (!inBounds(nc, nr)) {
                    continue;
                }
                Tile tile = tiles[nr][nc];
                if (tile.getType() == TileType.WALL) {
                    // 四邻更适合做稳定高台，对角按种子稀疏保留
                    boolean cardinal = d[0] == 0 || d[1] == 0;
                    if (cardinal || rng.nextDouble() < 0.35) {
                        tile.setType(TileType.HIGH);
                    }
                }
            }
        }
    }

    private int randomLane(Random rng, int avoid) {
        int y;
        int guard = 0;
        do {
            y = 1 + rng.nextInt(rows - 2);
            guard++;
        } while (Math.abs(y - avoid) < 3 && guard < 20);
        return y;
    }

    private void addLine(int x1, int y1, int x2, int y2) {
        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);
        int x = x1;
        int y = y1;
        path.addCell(x, y);
        while (x != x2 || y != y2) {
            x += dx;
            y += dy;
            path.addCell(x, y);
        }
    }

    public void draw(Graphics2D g) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                drawTile(g, tiles[r][c]);
            }
        }
        drawPathRibbon(g);
        drawPortals(g);
    }

    private void drawTile(Graphics2D g, Tile tile) {
        int x = tile.getCol() * Config.TILE_SIZE;
        int y = tile.getRow() * Config.TILE_SIZE;
        int s = Config.TILE_SIZE;
        switch (tile.getType()) {
            case WALL:
                g.setColor(new Color(18, 28, 42));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(28, 42, 62));
                g.drawRect(x, y, s, s);
                // 实验室墙面纹理
                g.setColor(new Color(36, 54, 78, 90));
                g.fillRect(x + 8, y + 10, s - 16, 4);
                break;
            case PATH:
            case SPAWN:
            case EXIT:
                g.setColor(new Color(214, 201, 168));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(196, 178, 140));
                g.drawRect(x, y, s, s);
                g.setColor(new Color(255, 255, 255, 35));
                g.fillRect(x + 2, y + 2, s - 4, s / 3);
                break;
            case HIGH:
                g.setColor(new Color(58, 92, 128));
                g.fillRect(x, y, s, s);
                g.setColor(new Color(92, 138, 176));
                g.fillRect(x + 4, y + 4, s - 8, s - 8);
                g.setColor(new Color(170, 210, 230, 80));
                g.drawRect(x + 6, y + 6, s - 12, s - 12);
                float glint = VisualClock.pulse(1.5, tile.getCol() * 0.8 + tile.getRow());
                if (glint > 0.72f) {
                    g.setColor(new Color(220, 245, 255, (int) (50 * glint)));
                    g.fillRect(x + 10, y + 9, 8, 2);
                }
                break;
            default:
                break;
        }
    }

    private void drawPathRibbon(Graphics2D g) {
        List<double[]> points = path.getWaypoints();
        if (points.size() < 2) {
            return;
        }
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(90, 170, 210, 55));
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
        }
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(70, 150, 200, 120));
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
        }
        float period = 22f;
        float phase = period - (float) ((VisualClock.seconds() * 42) % period);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[] {7f, 15f}, phase));
        g.setColor(new Color(220, 248, 255, 160));
        for (int i = 1; i < points.size(); i++) {
            double[] a = points.get(i - 1);
            double[] b = points.get(i);
            g.drawLine((int) a[0], (int) a[1], (int) b[0], (int) b[1]);
        }
    }

    private void drawPortals(Graphics2D g) {
        double[] spawn = path.getSpawnPoint();
        double[] exit = path.getExitPoint();
        float pulse = VisualClock.pulse(3.4, 0);
        drawPortal(g, spawn[0], spawn[1], new Color(70, 200, 255), new Color(180, 245, 255), "入", pulse);
        drawPortal(g, exit[0], exit[1], new Color(230, 80, 90), new Color(255, 190, 190), "终", VisualClock.pulse(3.4, 1.4));
    }

    private void drawPortal(Graphics2D g, double x, double y, Color fill, Color edge, String label, float pulse) {
        double ring = 20 + 8 * pulse;
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 90));
        g.drawOval((int) (x - ring), (int) (y - ring), (int) (ring * 2), (int) (ring * 2));
        RenderUtils.glow(g, x, y, 26 + 6 * pulse, new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 100));
        g.setFont(Fonts.SMALL);
        RenderUtils.fillCircle(g, x, y, 16, new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 180), edge);
        g.setColor(fill.getBlue() > fill.getRed() ? new Color(20, 40, 60) : new Color(60, 10, 20));
        RenderUtils.drawCenteredString(g, label, (int) x, (int) y);
    }
}

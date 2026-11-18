package game.map;

import game.Config;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 敌人行进路径：网格序列 + 像素路点（取地块中心）。
 * 敌人使用路点插值移动，无需运行时寻路。
 */
public class Path {

    private final List<Point> cells = new ArrayList<>();
    private final List<double[]> waypoints = new ArrayList<>();
    private double totalLength;

    public void addCell(int col, int row) {
        if (!cells.isEmpty()) {
            Point last = cells.get(cells.size() - 1);
            if (last.x == col && last.y == row) {
                return;
            }
        }
        cells.add(new Point(col, row));
        double x = (col + 0.5) * Config.TILE_SIZE;
        double y = (row + 0.5) * Config.TILE_SIZE;
        if (!waypoints.isEmpty()) {
            double[] prev = waypoints.get(waypoints.size() - 1);
            totalLength += Math.hypot(x - prev[0], y - prev[1]);
        }
        waypoints.add(new double[] {x, y});
    }

    public List<Point> getCells() {
        return Collections.unmodifiableList(cells);
    }

    public List<double[]> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    public double[] getSpawnPoint() {
        return waypoints.get(0);
    }

    public double[] getExitPoint() {
        return waypoints.get(waypoints.size() - 1);
    }

    public double getTotalLength() {
        return totalLength;
    }

    /**
     * 沿路径按弧长取点。超出两端时钳制在起点或终点。
     */
    public double[] pointAtDistance(double distance) {
        if (waypoints.isEmpty()) {
            return new double[] {0, 0};
        }
        if (distance <= 0 || waypoints.size() == 1) {
            double[] first = waypoints.get(0);
            return new double[] {first[0], first[1]};
        }
        double remain = distance;
        for (int i = 1; i < waypoints.size(); i++) {
            double[] a = waypoints.get(i - 1);
            double[] b = waypoints.get(i);
            double seg = Math.hypot(b[0] - a[0], b[1] - a[1]);
            if (remain <= seg || i == waypoints.size() - 1) {
                double t = seg <= 1e-6 ? 1 : Math.min(1, remain / seg);
                return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
            }
            remain -= seg;
        }
        double[] last = waypoints.get(waypoints.size() - 1);
        return new double[] {last[0], last[1]};
    }

    public int size() {
        return waypoints.size();
    }
}

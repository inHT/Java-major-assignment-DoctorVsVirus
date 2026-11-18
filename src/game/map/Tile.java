package game.map;

/**
 * 单个网格地块。
 */
public class Tile {

    private final int col;
    private final int row;
    private TileType type;
    private boolean occupied;

    public Tile(int col, int row, TileType type) {
        this.col = col;
        this.row = row;
        this.type = type;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public boolean isWalkable() {
        return type == TileType.PATH || type == TileType.SPAWN || type == TileType.EXIT;
    }
}

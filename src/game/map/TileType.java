package game.map;

/**
 * 地块类型。
 * PATH：敌人行进且近战单位可部署；HIGH：远程单位高台；SPAWN/EXIT 不可部署。
 */
public enum TileType {
    WALL,
    PATH,
    HIGH,
    SPAWN,
    EXIT
}

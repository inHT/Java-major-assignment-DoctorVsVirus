package game;

/**
 * 对局内状态。主菜单由独立面板承担，不进入此枚举。
 */
public enum GameState {
    /** 波次开始前的准备时间 */
    PREPARING,
    /** 敌人正在进攻 */
    COMBAT,
    /** 波次结束后的商店 */
    SHOP,
    PAUSED,
    VICTORY,
    DEFEAT
}

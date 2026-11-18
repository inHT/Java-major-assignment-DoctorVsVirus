package game;

/**
 * 全局常量和平衡数值。
 */
public final class Config {

    public static final String GAME_TITLE = "病区防线 Ward Defensee";
    public static final String GAME_TITLE_CN = "病区防线";
    public static final String GAME_TITLE_EN = "Ward Defensee";

    public static final int FPS = 60;

    public static final int TILE_SIZE = 48;
    public static final int MAP_COLS = 18;
    public static final int MAP_ROWS = 11;

    public static final int TOP_BAR = 56;
    public static final int BOTTOM_BAR = 152;
    public static final int SIDE_PAD = 20;

    public static final int MAP_PIXEL_WIDTH = MAP_COLS * TILE_SIZE;
    public static final int MAP_PIXEL_HEIGHT = MAP_ROWS * TILE_SIZE;
    public static final int WINDOW_WIDTH = SIDE_PAD * 2 + MAP_PIXEL_WIDTH;
    public static final int WINDOW_HEIGHT = TOP_BAR + MAP_PIXEL_HEIGHT + BOTTOM_BAR;

    public static final long DEFAULT_SEED = 20240915L;

    public static final int PLAYER_LIVES = 12;
    public static final int STARTING_DP = 24;
    public static final int MAX_DP = 99;
    public static final double DP_PER_SECOND = 1.0;
    public static final int STARTING_GOLD = 0;

    public static final int MAX_WAVES = 12;
    public static final double RETREAT_REFUND_RATE = 0.5;

    public static final double BLOCK_RADIUS = 0.72;
    public static final double TIME_SCALE_FAST = 2.0;

    /** 信息科对相邻友军的属性倍率 */
    public static final double INFO_ADJACENT_BONUS = 1.5;
    /** 骨科 / 放射科技能攻击强化 */
    public static final double SKILL_ATTACK_BONUS = 1.5;
    /** 护士技能按最大生命回血比例 */
    public static final double NURSE_SKILL_HEAL = 0.40;
    /** 外科技能降低敌方防御（受伤增加） */
    public static final double SURGEON_VULN = 0.30;
    /** 信息科二进制覆盖、停留、淡出时长（秒） */
    public static final double BINARY_COVER = 0.55;
    public static final double BINARY_HOLD = 0.18;
    public static final double BINARY_FADE = 0.45;
    public static final double BINARY_TOTAL = BINARY_COVER + BINARY_HOLD + BINARY_FADE;

    public static final double ARROW_FADE = 0.40;
    public static final double SKILL_ANNOUNCE = 1.5;
    public static final double SKILL_TOAST = 2.4;
    public static final double SCREEN_SHAKE = 0.48;

    private Config() {
    }
}

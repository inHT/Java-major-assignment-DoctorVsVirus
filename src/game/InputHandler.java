package game;

import game.units.UnitType;

import java.awt.event.KeyEvent;

/**
 * 键盘与鼠标输入映射。
 */
public class InputHandler {

    private final GameWorld world;

    public InputHandler(GameWorld world) {
        this.world = world;
    }

    public void onMouseMoved(int mx, int my) {
        int col = (mx - Config.SIDE_PAD) / Config.TILE_SIZE;
        int row = (my - Config.TOP_BAR) / Config.TILE_SIZE;
        if (mx < Config.SIDE_PAD || my < Config.TOP_BAR) {
            col = -1;
            row = -1;
        }
        world.setHoverTile(col, row);
    }

    public void onLeftClick() {
        if (world.isRetreatMode()) {
            if (!world.tryRetreatAtHover()) {
                world.setBanner("请点击要取消部署的医生");
            }
            return;
        }
        if (world.tryCastSkillAtHover()) {
            return;
        }
        world.tryDeploy();
    }

    public void onRightClick() {
        if (world.unitAt(world.getHoverCol(), world.getHoverRow()) != null) {
            world.tryRetreatAtHover();
        } else {
            world.clearSelection();
        }
    }

    public void onKeyPressed(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_1:
                world.selectType(UnitType.ORTHOPEDIC);
                break;
            case KeyEvent.VK_2:
                world.selectType(UnitType.RADIOLOGY);
                break;
            case KeyEvent.VK_3:
                world.selectType(UnitType.PROTECTIVE);
                break;
            case KeyEvent.VK_4:
                world.selectType(UnitType.NURSE);
                break;
            case KeyEvent.VK_5:
                world.selectType(UnitType.ANESTHESIA);
                break;
            case KeyEvent.VK_6:
                world.selectType(UnitType.SURGEON);
                break;
            case KeyEvent.VK_7:
                world.selectType(UnitType.INFO);
                break;
            case KeyEvent.VK_E:
                world.tryCastSelectedSkill();
                break;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:
                world.clearSelection();
                break;
            case KeyEvent.VK_SPACE:
                world.togglePause();
                break;
            case KeyEvent.VK_F:
                world.toggleFastForward();
                break;
            default:
                break;
        }
    }
}

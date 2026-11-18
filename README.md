# 病区防线 Ward Defensee

纯 Java 实现的 2D 塔防游戏：病毒沿固定路径冲击终点，玩家在路径与高台部署医疗单位拦截。

## 环境要求

- JDK 8 及以上
- 不依赖游戏引擎。MP3 由源码中附带的 JLayer 解码器一并编译，无需再安装音频库
- 地图与特效用 `Graphics2D` 绘制；角色立绘来自 `游戏资产/角色立绘`；音乐和音效来自 `游戏资产/BGM`、`游戏资产/音效`

## 编译与运行

在 `DoctorVsVirus` 目录下执行：

**Windows（命令提示符）**

```bat
compile.bat
```

或手动编译：

```bat
if not exist out mkdir out
javac -encoding UTF-8 -d out src/game/*.java src/game/audio/*.java src/game/map/*.java src/game/units/*.java src/game/enemies/*.java src/game/system/*.java src/game/ui/*.java src/javazoom/jl/decoder/*.java
java -cp out game.Main
```

**PowerShell**

```powershell
New-Item -ItemType Directory -Force -Path out | Out-Null
javac -encoding UTF-8 -d out src/game/*.java src/game/audio/*.java src/game/map/*.java src/game/units/*.java src/game/enemies/*.java src/game/system/*.java src/game/ui/*.java src/javazoom/jl/decoder/*.java
java -cp out game.Main
```

## 主界面

- **开始游戏**：进入关卡（可填写地图种子）。开局先对准入侵起点，沿路径运镜到终点，再拉远展示整张地图，然后才开始准备计时。空格可跳过这段预览
- **玩法**：说明机制、角色定位与技能（不含具体数值）
- **退出**：关闭游戏
- 关卡左上角可 **返回主界面**

## 音乐与音效

启动后循环播放 `游戏资产/BGM` 里的曲目，主界面和对局共用。音效按文件名接到对应时机：医生部署、普通攻击命中、敌人被击杀、一波结束打开商店、信息科医生发动技能。同一时刻攻击或击杀很多下时会稍作间隔，避免声音叠成一片。

## 操作

| 操作 | 作用 |
| --- | --- |
| 数字键 1-7 | 选择单位 |
| 鼠标左键 | 在合法地块部署；点击己方单位释放技能 |
| 鼠标右键 | 撤回已部署单位（返还 50% 部署点）或取消选择 |
| 右下角「取消部署」 | 再点击场上医生，取消部署并返还 50% 部署点 |
| E | 释放当前所选类型的就绪技能 |
| 空格 | 暂停 / 继续（商店中为进入下一波） |
| F | 切换 2 倍速 |
| Esc / Q | 取消当前选择 |

## 单位

| 热键 | 单位 | 部署位置 | 职责 / 技能 |
| --- | --- | --- | --- |
| 1 | 骨科医生 | 路径 | 近战阻挡；技能强化自身并增加射程 |
| 2 | 放射科医生 | 高台 | 远程输出；技能强化后攻击全体敌人 |
| 3 | 防护服医生 | 路径 | 高耐久阻挡；技能期间无敌 |
| 4 | 护士 | 路径或高台 | 治疗周围友军；技能全体回血 |
| 5 | 麻醉科医生 | 高台 | 减速干扰；技能全场减速 |
| 6 | 外科医生 | 路径 | 高频近战；技能削弱敌方防御 |
| 7 | 信息科医生 | 路径或高台 | 强化相邻医生；技能清除场上全部敌人 |

## 敌人

大肠杆菌 → 流感病毒 → 天花病毒 → 新冠病毒（残血加速）→ 艾滋病病毒（削弱附近医生攻击）→ 埃博拉病毒（Boss，残血狂暴）。

共 12 波。生命归零失败，清完所有波次胜利。击杀获得部署资源与金币；每波结束后可在商店购买永久增益。

## 地图

默认种子 `20240915`。主菜单可修改种子：相同种子始终生成相同路径与高台布局。

## 项目结构

```
src/game/           入口、窗口、面板、循环、世界、输入
src/game/audio/     音乐与音效
src/game/map/       地图、地块、路径
src/game/units/     己方单位
src/game/enemies/   敌方单位
src/game/system/    波次、资源、战斗、商店、弹道与特效
src/game/ui/        菜单、HUD、绘制工具
src/javazoom/jl/    MP3 解码（JLayer，LGPL）
游戏资产/角色立绘/    医生立绘
游戏资产/BGM/        背景音乐
游戏资产/音效/        部署、攻击、击杀、商店、信息科技能
```

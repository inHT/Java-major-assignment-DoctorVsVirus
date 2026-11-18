package game.system;

import game.enemies.EnemyType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 波次配置与生成节奏。准备时间结束后按时间表刷怪。
 */
public class WaveManager {

    public static final class SpawnEvent {
        public final double time;
        public final EnemyType type;

        public SpawnEvent(double time, EnemyType type) {
            this.time = time;
            this.type = type;
        }
    }

    public static final class Wave {
        public final int index;
        public final double prepareSeconds;
        public final List<SpawnEvent> events;

        public Wave(int index, double prepareSeconds, List<SpawnEvent> events) {
            this.index = index;
            this.prepareSeconds = prepareSeconds;
            this.events = events;
            this.events.sort(Comparator.comparingDouble(e -> e.time));
        }

        public double duration() {
            if (events.isEmpty()) {
                return 0;
            }
            return events.get(events.size() - 1).time + 0.5;
        }
    }

    private final List<Wave> waves = new ArrayList<>();
    private int currentWaveIndex;
    private double prepareRemain;
    private double combatTime;
    private int nextEventIndex;
    private boolean waveSpawning;
    private boolean allWavesCleared;

    public WaveManager() {
        buildWaves();
        currentWaveIndex = 0;
        prepareRemain = waves.get(0).prepareSeconds;
        waveSpawning = false;
    }

    public void beginNextPrepare() {
        if (currentWaveIndex >= waves.size()) {
            allWavesCleared = true;
            return;
        }
        prepareRemain = waves.get(currentWaveIndex).prepareSeconds;
        combatTime = 0;
        nextEventIndex = 0;
        waveSpawning = false;
    }

    /**
     * @return 本帧应当生成的敌人类型列表
     */
    public List<EnemyType> update(double dt, boolean inPrepare) {
        List<EnemyType> spawned = new ArrayList<>();
        if (allWavesCleared) {
            return spawned;
        }
        if (inPrepare) {
            prepareRemain -= dt;
            return spawned;
        }
        if (!waveSpawning) {
            waveSpawning = true;
            combatTime = 0;
            nextEventIndex = 0;
        }
        combatTime += dt;
        Wave wave = waves.get(currentWaveIndex);
        while (nextEventIndex < wave.events.size() && wave.events.get(nextEventIndex).time <= combatTime) {
            spawned.add(wave.events.get(nextEventIndex).type);
            nextEventIndex++;
        }
        return spawned;
    }

    public boolean isPrepareFinished() {
        return prepareRemain <= 0;
    }

    public boolean isCurrentWaveSpawnFinished() {
        if (currentWaveIndex >= waves.size()) {
            return true;
        }
        return nextEventIndex >= waves.get(currentWaveIndex).events.size();
    }

    public void completeCurrentWave() {
        currentWaveIndex++;
        if (currentWaveIndex >= waves.size()) {
            allWavesCleared = true;
        }
    }

    public boolean isAllWavesCleared() {
        return allWavesCleared;
    }

    public int getDisplayWaveNumber() {
        return Math.min(currentWaveIndex + 1, waves.size());
    }

    public int getTotalWaves() {
        return waves.size();
    }

    public double getPrepareRemain() {
        return Math.max(0, prepareRemain);
    }

    public boolean isSpawning() {
        return waveSpawning;
    }

    public Wave getCurrentWave() {
        if (currentWaveIndex >= waves.size()) {
            return waves.get(waves.size() - 1);
        }
        return waves.get(currentWaveIndex);
    }

    private void buildWaves() {
        waves.add(wave(1, 14, group(0.0, 1.05, EnemyType.ECOLI, 16)));
        waves.add(wave(2, 10, group(0.0, 0.75, EnemyType.ECOLI, 20)));
        waves.add(wave(3, 10,
                group(0.0, 0.80, EnemyType.ECOLI, 12),
                group(1.5, 0.85, EnemyType.FLU, 12)));
        waves.add(wave(4, 10, group(0.0, 0.70, EnemyType.FLU, 18)));
        waves.add(wave(5, 9,
                group(0.0, 0.80, EnemyType.FLU, 12),
                group(2.5, 0.95, EnemyType.SMALLPOX, 10)));
        waves.add(wave(6, 9,
                group(0.0, 0.55, EnemyType.ECOLI, 20),
                group(3.0, 0.90, EnemyType.SMALLPOX, 12)));
        waves.add(wave(7, 10,
                group(0.0, 1.00, EnemyType.SMALLPOX, 12),
                group(2.0, 1.10, EnemyType.COVID19, 10)));
        waves.add(wave(8, 10, group(0.0, 0.85, EnemyType.COVID19, 16)));
        waves.add(wave(9, 10,
                group(0.0, 0.70, EnemyType.FLU, 16),
                group(3.0, 1.20, EnemyType.HIV, 8)));
        waves.add(wave(10, 9,
                group(0.0, 1.00, EnemyType.COVID19, 10),
                group(2.5, 1.15, EnemyType.HIV, 10)));
        waves.add(wave(11, 12,
                group(0.0, 0.45, EnemyType.ECOLI, 20),
                group(4.0, 0.90, EnemyType.SMALLPOX, 12),
                group(7.0, 1.00, EnemyType.COVID19, 8),
                group(10.0, 1.10, EnemyType.HIV, 6)));
        waves.add(wave(12, 16,
                group(0.0, 0.55, EnemyType.ECOLI, 16),
                group(4.0, 1.00, EnemyType.HIV, 8),
                group(8.0, 0.0, EnemyType.EBOLA, 2),
                group(12.0, 0.90, EnemyType.COVID19, 12),
                group(18.0, 0.0, EnemyType.EBOLA, 2)));
    }

    @SafeVarargs
    private final Wave wave(int index, double prepare, List<SpawnEvent>... groups) {
        List<SpawnEvent> events = new ArrayList<>();
        for (List<SpawnEvent> group : groups) {
            events.addAll(group);
        }
        return new Wave(index, prepare, events);
    }

    private List<SpawnEvent> group(double offset, double interval, EnemyType type, int count) {
        List<SpawnEvent> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double t = offset + i * Math.max(interval, 0.05);
            list.add(new SpawnEvent(t, type));
        }
        return list;
    }
}

package game.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerHook;
import javazoom.jl.decoder.JavaLayerUtils;
import javazoom.jl.decoder.SampleBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 背景音乐与音效。读取游戏资产里的 BGM 与音效，MP3 在启动时解码成 PCM。
 */
public final class Audio {

    public enum Cue {
        DEPLOY(0),
        KILL(40_000_000L),
        ATTACK(90_000_000L),
        SHOP(0),
        INFO(0),
        CLICK(0);

        final long gapNanos;

        Cue(long gapNanos) {
            this.gapNanos = gapNanos;
        }
    }

    private static final Map<Cue, Sample> CUES = new EnumMap<Cue, Sample>(Cue.class);
    private static final List<Sample> BGM = new ArrayList<Sample>();
    private static volatile boolean started;
    private static volatile boolean ready;
    private static boolean sfxErrorPrinted;

    private Audio() {
    }

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        Thread loader = new Thread(new Runnable() {
            public void run() {
                load();
            }
        }, "audio-load");
        loader.setDaemon(true);
        loader.start();
    }

    static boolean isReady() {
        return ready;
    }

    static int cueCount() {
        synchronized (CUES) {
            return CUES.size();
        }
    }

    static int bgmCount() {
        synchronized (BGM) {
            return BGM.size();
        }
    }

    public static void play(Cue cue) {
        if (cue == null) {
            return;
        }
        Sample sample;
        synchronized (CUES) {
            sample = CUES.get(cue);
        }
        if (sample != null) {
            sample.play();
        }
    }

    private static void load() {
        try {
            installDecoderHook();
            File sfxDir = assetDir("音效");
            File bgmDir = assetDir("BGM");
            if (sfxDir == null) {
                System.err.println("未找到音效目录：游戏资产/音效");
            } else {
                loadCues(sfxDir);
            }
            if (bgmDir == null) {
                System.err.println("未找到音乐目录：游戏资产/BGM");
            } else {
                loadBgm(bgmDir);
            }
            reportMissing();
            if (bgmCount() > 0) {
                startBgmThread();
            }
        } catch (Throwable ex) {
            System.err.println("音频初始化失败: " + ex.getMessage());
        } finally {
            ready = true;
        }
    }

    private static void reportMissing() {
        Cue[] cues = {Cue.DEPLOY, Cue.KILL, Cue.ATTACK, Cue.SHOP, Cue.INFO, Cue.CLICK};
        String[] names = {"部署", "击杀", "攻击", "商店", "信息科", "点击"};
        for (int i = 0; i < cues.length; i++) {
            if (!CUES.containsKey(cues[i])) {
                System.err.println("未加载音效：" + names[i]);
            }
        }
        if (BGM.isEmpty()) {
            System.err.println("未加载背景音乐");
        }
    }

    private static void loadCues(File dir) {
        for (File file : listAudio(dir)) {
            Cue cue = cueFor(file.getName());
            if (cue == null) {
                continue;
            }
            Sample sample = decode(file);
            if (sample == null) {
                continue;
            }
            sample.gapNanos = cue.gapNanos;
            synchronized (CUES) {
                CUES.put(cue, sample);
            }
        }
    }

    private static void loadBgm(File dir) {
        for (File file : listAudio(dir)) {
            Sample sample = decode(file);
            if (sample != null) {
                synchronized (BGM) {
                    BGM.add(sample);
                }
            }
        }
    }

    private static Cue cueFor(String name) {
        if (name.contains("点击")) {
            return Cue.CLICK;
        }
        if (name.contains("信息科")) {
            return Cue.INFO;
        }
        if (name.contains("部署")) {
            return Cue.DEPLOY;
        }
        if (name.contains("击杀")) {
            return Cue.KILL;
        }
        if (name.contains("商店")) {
            return Cue.SHOP;
        }
        if (name.contains("攻击")) {
            return Cue.ATTACK;
        }
        return null;
    }

    private static File[] listAudio(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return new File[0];
        }
        List<File> list = new ArrayList<File>();
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".au")
                    || name.endsWith(".aiff") || name.endsWith(".aif")) {
                list.add(file);
            }
        }
        File[] out = list.toArray(new File[list.size()]);
        Arrays.sort(out, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return out;
    }

    private static File assetDir(String child) {
        String userDir = System.getProperty("user.dir");
        File[] roots = {
                new File("游戏资产"),
                new File("DoctorVsVirus", "游戏资产"),
                new File(userDir, "游戏资产"),
                new File(new File(userDir, "DoctorVsVirus"), "游戏资产")
        };
        for (File assetRoot : roots) {
            File dir = new File(assetRoot, child);
            if (dir.isDirectory()) {
                return dir;
            }
        }
        return null;
    }

    private static Sample decode(File file) {
        try {
            if (isRiffWave(file)) {
                return decodePcmFile(file);
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".mp3")) {
                return decodeMp3(file);
            }
            return decodePcmFile(file);
        } catch (Exception ex) {
            System.err.println("无法解码 " + file.getName() + "：" + ex.getMessage());
            return null;
        }
    }

    /** 扩展名可能是 mp3，内容仍可能是 WAV。 */
    private static boolean isRiffWave(File file) {
        try (InputStream in = new FileInputStream(file)) {
            byte[] head = new byte[12];
            int got = 0;
            while (got < head.length) {
                int n = in.read(head, got, head.length - got);
                if (n < 0) {
                    break;
                }
                got += n;
            }
            return got >= 12
                    && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'A' && head[10] == 'V' && head[11] == 'E';
        } catch (IOException ex) {
            return false;
        }
    }

    private static Sample decodeMp3(File file) throws Exception {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        Bitstream bitstream = new Bitstream(in);
        Decoder decoder = new Decoder();
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        AudioFormat format = null;
        try {
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer buffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                short[] samples = buffer.getBuffer();
                int len = buffer.getBufferLength();
                if (format == null && len > 0) {
                    format = new AudioFormat(buffer.getSampleFrequency(), 16, buffer.getChannelCount(), true, false);
                }
                for (int i = 0; i < len; i++) {
                    short value = samples[i];
                    pcm.write(value & 0xff);
                    pcm.write((value >> 8) & 0xff);
                }
                bitstream.closeFrame();
            }
        } finally {
            bitstream.close();
        }
        if (format == null || pcm.size() == 0) {
            throw new IOException("没有可播放的采样");
        }
        return new Sample(format, align(pcm.toByteArray(), format.getFrameSize()));
    }

    private static Sample decodePcmFile(File file) throws Exception {
        AudioInputStream raw = AudioSystem.getAudioInputStream(file);
        try {
            AudioFormat base = raw.getFormat();
            AudioFormat pcm = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(),
                    16,
                    base.getChannels(),
                    base.getChannels() * 2,
                    base.getSampleRate(),
                    false);
            AudioInputStream decoded = base.matches(pcm) ? raw : AudioSystem.getAudioInputStream(pcm, raw);
            try {
                byte[] data = align(readAll(decoded), pcm.getFrameSize());
                if (data.length == 0) {
                    throw new IOException("没有可播放的采样");
                }
                return new Sample(pcm, data);
            } finally {
                if (decoded != raw) {
                    decoded.close();
                }
            }
        } finally {
            raw.close();
        }
    }

    private static byte[] align(byte[] data, int frame) {
        if (frame <= 1 || data.length % frame == 0) {
            return data;
        }
        int usable = data.length - (data.length % frame);
        byte[] cut = new byte[usable];
        System.arraycopy(data, 0, cut, 0, usable);
        return cut;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            if (n > 0) {
                out.write(buf, 0, n);
            }
        }
        return out.toByteArray();
    }

    private static void installDecoderHook() {
        JavaLayerUtils.setHook(new JavaLayerHook() {
            public InputStream getResourceAsStream(String name) {
                try {
                    InputStream classpath = JavaLayerUtils.class.getResourceAsStream(name);
                    if (classpath != null) {
                        try {
                            return new ByteArrayInputStream(readAll(classpath));
                        } finally {
                            classpath.close();
                        }
                    }
                } catch (IOException ignored) {
                    return null;
                }
                String userDir = System.getProperty("user.dir");
                File[] files = {
                        new File("src/javazoom/jl/decoder", name),
                        new File("DoctorVsVirus/src/javazoom/jl/decoder", name),
                        new File(userDir, "src/javazoom/jl/decoder/" + name),
                        new File(new File(userDir, "DoctorVsVirus"), "src/javazoom/jl/decoder/" + name)
                };
                for (File file : files) {
                    if (!file.isFile()) {
                        continue;
                    }
                    try {
                        InputStream in = new FileInputStream(file);
                        try {
                            return new ByteArrayInputStream(readAll(in));
                        } finally {
                            in.close();
                        }
                    } catch (IOException ignored) {
                        // 试下一个位置
                    }
                }
                return null;
            }
        });
    }

    private static void startBgmThread() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                loopBgm();
            }
        }, "bgm");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loopBgm() {
        while (!Thread.currentThread().isInterrupted()) {
            List<Sample> tracks;
            synchronized (BGM) {
                tracks = new ArrayList<Sample>(BGM);
            }
            if (tracks.isEmpty()) {
                return;
            }
            for (Sample sample : tracks) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                SourceDataLine line = null;
                try {
                    line = AudioSystem.getSourceDataLine(sample.format);
                    line.open(sample.format);
                    setGain(line, -10f);
                    line.start();
                    byte[] data = sample.data;
                    int off = 0;
                    while (off < data.length) {
                        int wrote = line.write(data, off, data.length - off);
                        if (wrote <= 0) {
                            break;
                        }
                        off += wrote;
                    }
                    line.drain();
                } catch (Exception ex) {
                    System.err.println("背景音乐播放失败: " + ex.getMessage());
                    return;
                } finally {
                    if (line != null) {
                        line.stop();
                        line.close();
                    }
                }
            }
        }
    }

    private static void setGain(Line line, float db) {
        if (!line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl control = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        float value = Math.max(control.getMinimum(), Math.min(control.getMaximum(), db));
        control.setValue(value);
    }

    private static final class Sample {
        final AudioFormat format;
        final byte[] data;
        long gapNanos;
        long lastPlay;
        int opened;
        final ArrayDeque<Clip> idle = new ArrayDeque<Clip>();

        Sample(AudioFormat format, byte[] data) {
            this.format = format;
            this.data = data;
        }

        void play() {
            long now = System.nanoTime();
            synchronized (this) {
                if (gapNanos > 0 && now - lastPlay < gapNanos) {
                    return;
                }
                lastPlay = now;
            }
            try {
                Clip clip;
                synchronized (idle) {
                    clip = idle.pollFirst();
                }
                if (clip == null) {
                    synchronized (this) {
                        if (opened >= 6) {
                            return;
                        }
                        opened++;
                    }
                    final Clip created = AudioSystem.getClip();
                    created.open(format, data, 0, data.length);
                    setGain(created, 0f);
                    created.addLineListener(new LineListener() {
                        public void update(LineEvent event) {
                            if (event.getType() != LineEvent.Type.STOP || created.isRunning()) {
                                return;
                            }
                            created.setFramePosition(0);
                            synchronized (idle) {
                                if (!idle.contains(created)) {
                                    idle.addLast(created);
                                }
                            }
                        }
                    });
                    clip = created;
                } else {
                    clip.setFramePosition(0);
                }
                clip.start();
            } catch (Exception ex) {
                if (!sfxErrorPrinted) {
                    sfxErrorPrinted = true;
                    System.err.println("音效播放失败: " + ex.getMessage());
                }
            }
        }
    }
}

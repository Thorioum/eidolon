package net.thorioum;

import net.thorioum.gui.Theme;
import net.thorioum.gui.Window;
import net.thorioum.result.CompleteAudioResult;
import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundEffectDatabase;
import net.thorioum.sound.SoundFilesGrabber;
import net.thorioum.sound.SoundMatcher;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Eidolon {
    public static void initialize() {
        SoundFilesGrabber.init();
    }

    private static Window window;

    public static void main(String[] args) {
        initialize();

        List<MinecraftVersion> supportedVersions = SoundFilesGrabber.soundMap.keySet().stream().toList();

        SwingUtilities.invokeLater(() -> {
            window = new Window(supportedVersions);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            Theme.apply();

        });

    }




    public static ExecutorService executor = null;
    private static int lastNum = -1;
    static {
        resetExecutor();
    }
    public static void resetExecutor() {
        resetExecutor(lastNum == -1 ? Runtime.getRuntime().availableProcessors()/2 : lastNum);
    }
    public static void resetExecutor(int numProcessors) {
        int num = Math.min(Runtime.getRuntime().availableProcessors(),numProcessors);
        lastNum = num;
        if(executor != null) executor.shutdownNow();
        executor = Executors.newFixedThreadPool(num);
    }

    public static void info(String message, Object... args) {
        String str = "[INFO]: " + String.format(message,args) + "\n";
        Window.consoleArea.append(str);
        System.out.println(str);
    }
    public static void error(String message, Object... args) {
        String str = "[ERROR]: " + String.format(message,args) + "\n";
        Window.consoleArea.append(str);
        System.err.println(str);
    }

    private static boolean soundCheck(ConverterContext ctx) {
        if(ctx.sounds() == null) {
            error("this shouldnt happen");
        }
        if(ctx.frameLength() % 50 != 0 && !SoundFilesGrabber.allowsDynamicTickrate(ctx.version())) {
            error("Because your select version is below 23w43a (1.20.3 snapshot), and /tick doesnt exist yet, your framelength can only be a multiple of 50ms!");
            return true;
        }
        return false;
    }

    private static final CompleteAudioResult resultBuffer = new CompleteAudioResult();
    public static CompleteAudioResult getCurrentResult() {
        return resultBuffer;
    }
    public static SoundEffectDatabase currentDb = null;
    public static ConverterContext currentContext = null;
    public static SoundMatcher currentMatcher = null;

    public static Status processingStatus = Status.IDLE;
    public static boolean process(ConverterContext ctx, int soundsPerFrame, boolean useGpu, List<String> blacklistedSounds, File file) {
        if(soundCheck(ctx)) return false;
        resetExecutor();
        resultBuffer.clear();
        currentContext = ctx;

        processingStatus = Status.PROCESSING_SOUNDS;
        currentDb = SoundMatcher.getDatabase(ctx);
        currentDb.processSounds(ctx);
        if(processingStatus == Status.IDLE) return false;

        currentMatcher = new SoundMatcher(resultBuffer, useGpu);

        currentMatcher.enableMatchers(ctx,blacklistedSounds);

        processingStatus = Status.MATCHING_AUDIO;
        currentMatcher.processAudioFile(ctx,soundsPerFrame,file,(result)->{

        });
        processingStatus = Status.COMPLETE;
        return true;
    }

    public static void cancelCurrentProcess() {
        Eidolon.processingStatus = Status.IDLE;
        resetExecutor();
    }
    public enum Status {
        IDLE,
        PROCESSING_SOUNDS,
        MATCHING_AUDIO,
        COMPLETE
    }
}

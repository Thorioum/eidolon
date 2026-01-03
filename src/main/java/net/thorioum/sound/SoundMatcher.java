package net.thorioum.sound;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import net.thorioum.Eidolon;
import net.thorioum.matchers.GreedySubCpuMatcher;
import net.thorioum.matchers.GreedySubGpuMatcher;
import net.thorioum.result.CompleteAudioResult;
import net.thorioum.result.SingleFrameResult;
import net.thorioum.result.SingleSoundResult;

import static net.thorioum.Eidolon.*;
import static net.thorioum.sound.SoundEffectDatabase.SAMPLE_RATE;


public class SoundMatcher {
    public final CompleteAudioResult result;
    private final boolean useGpu;

    public SoundMatcher(CompleteAudioResult buffer, boolean useGpu) {
        this.result = buffer;
        this.useGpu = useGpu;
    }

    private boolean ended = false;

    //different database for different contexts are cached
    private static final Map<ConverterContext, SoundEffectDatabase> soundEffectDatabaseMap = new ConcurrentHashMap<>();

    public static SoundEffectDatabase getDatabase(ConverterContext ctx) {
        synchronized (soundEffectDatabaseMap) {
            return soundEffectDatabaseMap.computeIfAbsent(ctx, f-> new SoundEffectDatabase());
        }
    }


    private static volatile GreedySubGpuMatcher GPU;
    private static volatile GreedySubCpuMatcher CPU;

    public static synchronized void freeCurrentGPU() {
        if (GPU != null) {
            GPU.release();
            GPU = null;
        }
    }
    public synchronized void initializeGpuMatcher(ConverterContext ctx, List<String> blacklistedSounds) {
        if(!useGpu) return;
        freeCurrentGPU();
        if (GPU == null) GPU = new GreedySubGpuMatcher();
        try {
            GPU.buildFromDatabase(getDatabase(ctx), ctx.frameSize(), blacklistedSounds);
        } catch (Throwable t) {
            t.printStackTrace();
            error("GPU unavailable");
            GPU = null;
        }

    }
    public synchronized void initializeCpuMatcher(ConverterContext ctx, List<String> blacklistedSounds) {
        if (CPU != null) return;

        GreedySubCpuMatcher matcher = new GreedySubCpuMatcher();
        matcher.buildFromDatabase(getDatabase(ctx), ctx.frameSize(), blacklistedSounds);
        CPU = matcher;
    }



    public void processAudioFile(ConverterContext ctx, int soundsPerFrame, File audioFile, Consumer<CompleteAudioResult> soundsConsumer) {

        try {
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(
                    audioFile.getAbsolutePath(),
                    SAMPLE_RATE,
                    ctx.frameSize(),
                    0
            );

            List<Future<?>> futures = new ArrayList<>();

            SoundMatcher this$0 = this;
            dispatcher.addAudioProcessor(new AudioProcessor() {

                int frame = 0;

                @Override
                public boolean process(AudioEvent audioEvent) {
                    final int frame = this.frame++;
                    double[] audioBuffer = new double[audioEvent.getFloatBuffer().length];
                    for (int i = 0; i < audioEvent.getFloatBuffer().length; i++) {
                        audioBuffer[i] = audioEvent.getFloatBuffer()[i];
                    }

                    futures.add(executor.submit(() -> {
                        if(this$0.ended) return;
                        SingleFrameResult composition = findBestComposition(ctx,audioBuffer, frame,soundsPerFrame);

                        result.addFrame(composition);

                    }));
                    return true;
                }

                @Override
                public void processingFinished() {
                    result.expectedFrames = futures.size();
                    for (Future<?> future : futures) {
                        try {
                            if(this$0.ended) {
                                Eidolon.resetExecutor();
                                return;
                            }
                            future.get();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(!ended) {
                        ended = true;
                        soundsConsumer.accept(result);
                    }
                    Eidolon.resetExecutor();
                }
            });

            dispatcher.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void enableMatchers(ConverterContext ctx, List<String> blacklistedSounds) {
        try {
            initializeGpuMatcher(ctx, blacklistedSounds);
        } catch (Throwable t) {
            t.printStackTrace();
            info("GPU acceleration unavailable");
        }
        try {
            initializeCpuMatcher(ctx, blacklistedSounds);
        } catch (Throwable t) {
            t.printStackTrace();
            info("CPU acceleration unavailable");
        }
    }

    private SingleFrameResult findBestComposition(ConverterContext ctx, double[] targetFrame, int frameNum, int totalSounds) {
        double[] residual  = Arrays.copyOf(targetFrame, targetFrame.length);
        Util.highPassInPlace(residual, ctx.highpass_cutoff());

        double originalEnergyHP = Util.calculateEnergy(residual);
        SingleFrameResult composition = new SingleFrameResult(frameNum);

        for (int i = 0; i < totalSounds; i++) {
            double residualEnergy = Util.calculateEnergy(residual);
            if (residualEnergy < originalEnergyHP * 0.05) break;

            SingleSoundResult match = findBestMatch(ctx, residual, residualEnergy);
            if (match == null || match.similarity() < 0.1) break;

            composition.addEffect(new SingleSoundResult(match.name(), match.pitch(), match.volume(),match.similarity(),match.audioData()));

            for (int j = 0; j < residual.length; j++) {
                residual[j]  -= match.audioData()[j]  * match.volume();
            }
        }
        return composition;
    }
    private SingleSoundResult findBestMatch(ConverterContext ctx, double[] residual, double residualEnergy) {
        float[] resF = new float[residual.length];
        for (int i = 0; i < residual.length; i++) resF[i] = (float) residual[i];

        if (useGpu && GPU != null && GPU.isReady()) {
            SingleSoundResult gm = GPU.findBestMatch(ctx, resF, residualEnergy, 0.1f);
            if (gm != null) return gm;
        }

        if (CPU != null && CPU.isReady()) {
            SingleSoundResult cm = CPU.findBestMatch(ctx, resF, residualEnergy, 0.1f);
            if (cm != null) return cm;
        }

        return null;
    }

}
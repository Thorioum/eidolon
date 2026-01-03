package net.thorioum.sound;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import net.thorioum.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static net.thorioum.Eidolon.*;

public class SoundEffectDatabase {
    public static final int SAMPLE_RATE = 48000;

    public final Map<String, Map<Double, double[]>> pitchShiftedEffects = new ConcurrentHashMap<>(); // raw
    public final Map<String, Map<Double, Double>> effectNorms = new ConcurrentHashMap<>();

    public int metaExpectedSounds = -1;
    public int metaProcessedSounds() {
        return effectNorms.size();
    }

    public void processSounds(ConverterContext ctx) {
        List<Future<?>> futures = new ArrayList<>();
        metaExpectedSounds = ctx.soundFilesMap().size();

        for (Map.Entry<String, File> entry : ctx.soundFilesMap().entrySet()) {
            if(effectNorms.containsKey(entry.getKey())) continue;
            futures.add(executor.submit(() -> {

                String name = entry.getKey();

                if(!pitchShiftedEffects.containsKey(name)) {
                    File file = entry.getValue();
                    loadSoundAndPitches(ctx,name, file);
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                if(!(e instanceof InterruptedException)) {
                    error(e.toString());
                    e.printStackTrace();
                }
            }
        }
        info("Processed all sound files! (" + this.pitchShiftedEffects.size() + " sounds)");
        info("Ignored sounds are either blacklisted, or are multi-channel/stereo");
    }


    public void loadSoundAndPitches(ConverterContext ctx, String name, File audioFile) {
        try {
            try {
                if (Util.getChannelCount(audioFile) != 1) {
                    return;
                }
            } catch (Exception ignored) {}


            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(
                    audioFile.getAbsolutePath(),
                    SAMPLE_RATE,
                    2048,
                    0
            );

            if(dispatcher.getFormat().getChannels() != 1) return;

            int pitchIntensity = ctx.pitchesPerSound();

            final List<Pair<Double,List<Double>>> pitchMap = new ArrayList<>();

            for (int i = 0; i < (pitchIntensity+1); i++) {
                double pitch = Util.pitchFunc(i,pitchIntensity);
                pitchMap.add(new Pair<>(pitch, new ArrayList<>()));
            }

            dispatcher.addAudioProcessor(new AudioProcessor() {

                @Override
                public boolean process(AudioEvent audioEvent) {

                    float[] buffer = audioEvent.getFloatBuffer().clone();

                    int samplesInBuffer = buffer.length;

                    boolean full = true;
                    for (int i = 0; i < (pitchIntensity+1); i++) {
                        List<Double> audioBuffer = pitchMap.get(i).right;
                        if (audioBuffer.size() < ctx.frameSize()) {
                            full = false;
                            break;
                        }
                    }
                    if(full) return false;


                    for (int i = 0; i < (pitchIntensity+1); i++) {

                        List<Double> audioBuffer = pitchMap.get(i).right;
                        if(audioBuffer.size() >= ctx.frameSize()) continue;

                        double pitch = Util.pitchFunc(i,pitchIntensity);
                        RateTransposer rateTransposer = new RateTransposer(pitch);

                        float[] outputBuffer = new float[(int) (samplesInBuffer / pitch)];
                        rateTransposer.process(buffer, outputBuffer);

                        for (float sample : outputBuffer) {
                            if (audioBuffer.size() < ctx.frameSize()) {
                                audioBuffer.add((double) sample);
                            } else {
                                break;
                            }
                        }

                    }

                    return true;
                }

                @Override
                public void processingFinished() {
                    for(Pair<Double, List<Double>> audioBuffer : pitchMap) {
                        if(audioBuffer.right.size() != ctx.frameSize()) continue;

                        Double pitch = 1.0 / audioBuffer.left;

                        float ingameSoundPitch = ctx.soundPitchesMap().get(name);

                        double adjustedPitch = (pitch * (1.0/(ingameSoundPitch+1e-10)));
                        if(adjustedPitch > 2 || adjustedPitch < 0.5) continue;

                        double[] audio = audioBuffer.right.stream()
                                .mapToDouble(Double::doubleValue)
                                .toArray();

                        float ingameVolume = ctx.soundVolumesMap().get(name);
                        for(int i = 0; i < audio.length; i++) {
                            audio[i] = audio[i] * ingameVolume;
                        }

                        if(Util.brightness(audio) > ctx.brightnessThreshold()) return;
                        double norm = Math.sqrt(Util.calculateEnergy(audio));

                        pitchShiftedEffects.computeIfAbsent(name, k->new HashMap<>()).put(pitch,audio);
                        effectNorms.computeIfAbsent(name, k -> new HashMap<>()).put(pitch, norm);

                    }
                }
            });

            dispatcher.run();

        } catch (Exception e) {
            error(e.toString());
            e.printStackTrace();
        }
    }
}

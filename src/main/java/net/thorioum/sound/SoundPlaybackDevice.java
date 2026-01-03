package net.thorioum.sound;

import net.thorioum.result.CompleteAudioResult;
import net.thorioum.result.SingleFrameResult;
import net.thorioum.result.SingleSoundResult;

import javax.sound.sampled.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.thorioum.Eidolon.error;
import static net.thorioum.sound.SoundEffectDatabase.SAMPLE_RATE;

public class SoundPlaybackDevice {

    private static volatile SourceDataLine activeLine;

    public static void stopPlaybackNow() {
        SourceDataLine line = activeLine;
        if (line != null) {
            try { line.stop(); } catch (Exception ignored) {}
            try { line.flush(); } catch (Exception ignored) {}
            try { line.close(); } catch (Exception ignored) {}
        }
    }


    public static void play(ConverterContext ctx, CompleteAudioResult result) {
        stopPlaybackNow();

        if(result.composition().isEmpty()) return;

        List<SingleFrameResult> frames = result.composition();

        Map<Integer, SingleFrameResult> byFrame = new HashMap<>();
        for (SingleFrameResult fr : frames) {
            byFrame.put(fr.frame, fr);
        }

        try {
            playRange(ctx, byFrame, frames.size());
        } catch (Exception e) {
            error(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void playRange(ConverterContext ctx, Map<Integer, SingleFrameResult> byFrame, int frameCount) throws LineUnavailableException {

        int bytesPerFrame = ctx.frameSize()*2;

        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                16,
                1,
                true,
                false
        );

        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));

        int lineBufferBytes = (int) (SAMPLE_RATE * bytesPerFrame * 0.1);
        line.open(format, lineBufferBytes);
        line.start();

        activeLine = line;
        try {

            double[] mix = new double[ctx.frameSize()];
            byte[] pcm = new byte[bytesPerFrame];
            byte[] silence = new byte[bytesPerFrame];
            for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
                SingleFrameResult frame = byFrame.get(frameIndex);

                if (frame == null || frame.getComposition().isEmpty()) {
                    line.write(silence, 0, silence.length);
                    continue;
                }

                List<SingleSoundResult> sounds = frame.getComposition();

                Arrays.fill(mix, 0.0);

                for (SingleSoundResult s : sounds) {
                    if (s == null) continue;
                    double[] data = s.audioData();
                    if (data == null) continue;
                    if (data.length != ctx.frameSize()) {
                        throw new IllegalArgumentException(
                                "Frame " + frameIndex + " sound '" + s.name() + "' has audioData length "
                                        + data.length + " but expected " + ctx.frameSize()
                        );
                    }

                    double vol = s.volume();
                    for (int i = 0; i < ctx.frameSize(); i++) {
                        mix[i] += data[i] * vol;
                    }
                }

                double peak = 0.0;
                for (int i = 0; i < ctx.frameSize(); i++) {
                    double a = Math.abs(mix[i]);
                    if (a > peak) peak = a;
                }
                double gain = (peak > 1.0) ? (0.99 / peak) : 1.0;

                int idx = 0;
                for (int i = 0; i < ctx.frameSize(); i++) {
                    double v = mix[i] * gain;

                    if (v > 1.0) v = 1.0;
                    if (v < -1.0) v = -1.0;

                    short s16 = (short) Math.round(v * 32767.0);
                    pcm[idx++] = (byte) (s16 & 0xFF);
                    pcm[idx++] = (byte) ((s16 >> 8) & 0xFF);
                }

                if(Thread.currentThread().isInterrupted())
                    return;

                line.write(pcm, 0, pcm.length);
            }

            line.drain();
        } finally {
            line.stop();
            line.close();
        }
    }
}

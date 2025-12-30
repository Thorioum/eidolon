package net.thorioum.sound;

import net.thorioum.MinecraftVersion;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.thorioum.sound.SoundEffectDatabase.SAMPLE_RATE;

public record ConverterContext(
        MinecraftVersion version,
        int frameLength,
        int pitchesPerSound,
        double highpass_cutoff,
        double brightnessThreshold,
        boolean allowStereoSounds,
        List<String> blacklistedSounds
) {
    public SoundFilesGrabber.SoundFilesEntry sounds() {
        return SoundFilesGrabber.soundMap.get(version);
    }

    public Map<String, Float> soundVolumesMap() {
        return sounds().soundVolumesMap();
    }
    public Map<String, Float> soundPitchesMap() {
        return sounds().soundPitchesMap();
    }
    public Map<String, File> soundFilesMap() {
        return sounds().soundFilesMap();
    }

    public int frameSize() {
        return (int) (SAMPLE_RATE * (frameLength / 1000.0f));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConverterContext that = (ConverterContext) o;
        return frameLength == that.frameLength && pitchesPerSound == that.pitchesPerSound && Double.compare(highpass_cutoff, that.highpass_cutoff) == 0 && allowStereoSounds == that.allowStereoSounds && Double.compare(brightnessThreshold, that.brightnessThreshold) == 0 && Objects.equals(version, that.version) && Objects.equals(blacklistedSounds, that.blacklistedSounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, frameLength, pitchesPerSound, highpass_cutoff, brightnessThreshold, allowStereoSounds, blacklistedSounds);
    }
}

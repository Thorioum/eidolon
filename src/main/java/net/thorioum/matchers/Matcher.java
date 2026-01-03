package net.thorioum.matchers;

import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundEffectDatabase;
import net.thorioum.result.SingleSoundResult;

import java.util.List;

public interface Matcher {
    boolean isReady();
    void buildFromDatabase(SoundEffectDatabase db, int frameSize, List<String> blacklistedSounds);
    SingleSoundResult findBestMatch(ConverterContext ctx, float[] residual, double residualEnergy, float minSim);
}

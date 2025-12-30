package net.thorioum.matchers;

import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundEffectDatabase;
import net.thorioum.result.SingleSoundResult;

public interface Matcher {
    boolean isReady();
    void buildFromDatabase(SoundEffectDatabase db, int frameSize);
    SingleSoundResult findBestMatch(ConverterContext ctx, float[] residual, double residualEnergy, float minSim);
}

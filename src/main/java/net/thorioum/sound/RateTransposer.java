package net.thorioum.sound;

import be.tarsos.dsp.resample.Resampler;

public class RateTransposer {
    private final double factor;
    private final Resampler r;

    public RateTransposer(double factor) {
        this.factor = factor;
        this.r = new Resampler(true, 0.1, 4.0);
    }

    public void process(float[] src, float[] out) {
        this.r.process(this.factor, src, 0, src.length, false, out, 0, out.length);
    }
}

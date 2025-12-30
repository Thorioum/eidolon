package net.thorioum.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

import static net.thorioum.sound.SoundEffectDatabase.SAMPLE_RATE;

public class SoundUtil {
    public static int getChannelCount(File audioFile)
            throws UnsupportedAudioFileException, IOException {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat fmt = in.getFormat();
            return fmt.getChannels(); // 1 = mono, 2 = stereo, etc.
        }
    }

    public static double brightness(double[] samples) {

        if (samples == null || samples.length == 0) return 0.0;

        int nOrig = samples.length;
        int n = nextPow2(Math.max(2, nOrig));

        double[] re = new double[n];
        double[] im = new double[n];

        for (int i = 0; i < nOrig; i++) {
            double w = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / Math.max(1, (nOrig - 1)));
            re[i] = samples[i] * w;
        }

        fft(re, im);

        int kStart = 1;
        int kEnd = n / 2;
        int bins = Math.max(1, kEnd - kStart + 1);

        double totalPower = 0.0;
        for (int k = kStart; k <= kEnd; k++) {
            double pk = re[k] * re[k] + im[k] * im[k];
            totalPower += pk;
        }
        if (totalPower <= 1e-20) return 0.0;

        double H = 0.0;
        for (int k = kStart; k <= kEnd; k++) {
            double pk = re[k] * re[k] + im[k] * im[k];
            double p = pk / totalPower;
            if (p > 0.0) H += -p * Math.log(p);
        }
        double Hmax = Math.log(bins);
        double brightness = (Hmax > 0.0) ? (H / Hmax) : 0.0;

        if (brightness < 0) brightness = 0;
        if (brightness > 1) brightness = 1;
        return brightness;
    }



    private static void fft(double[] re, double[] im) {
        int n = re.length;

        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >>> 1;
            for (; j >= bit; bit >>>= 1) j -= bit;
            j += bit;
            if (i < j) {
                double tr = re[i]; re[i] = re[j]; re[j] = tr;
                double ti = im[i]; im[i] = im[j]; im[j] = ti;
            }
        }

        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2.0 * Math.PI / len;
            double wlenRe = Math.cos(ang);
            double wlenIm = Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                double wr = 1.0, wi = 0.0;
                int half = len >>> 1;
                for (int j = 0; j < half; j++) {
                    int u = i + j;
                    int v = u + half;

                    double ur = re[u], ui = im[u];
                    double vr = re[v] * wr - im[v] * wi;
                    double vi = re[v] * wi + im[v] * wr;

                    re[u] = ur + vr; im[u] = ui + vi;
                    re[v] = ur - vr; im[v] = ui - vi;

                    double nwr = wr * wlenRe - wi * wlenIm;
                    wi = wr * wlenIm + wi * wlenRe;
                    wr = nwr;
                }
            }
        }
    }

    private static int nextPow2(int x) {
        int n = 1;
        while (n < x) n <<= 1;
        return n;
    }

    public static double pitchFunc(int index, int pitchIntensity) {
        double x = (double) index / pitchIntensity;
        return (1.0 / (-1.5*x - 0.5)) + 2.5;
    }

    public static double calculateEnergy(double[] samples) {
        double energy = 0.0;
        for (double sample : samples) {
            energy += sample * sample;
        }
        return energy;
    }

    public static void highPassInPlace(double[] x, double cutoffHz) {
        if(cutoffHz <= 0.0) return;
        double dt = 1.0 / SAMPLE_RATE;
        double rc = 1.0 / (2.0 * Math.PI * cutoffHz);
        double alpha = rc / (rc + dt);
        double prevY = 0.0, prevX = x[0];
        for (int i = 0; i < x.length; i++) {
            double y = alpha * (prevY + x[i] - prevX);
            prevY = y; prevX = x[i];
            x[i] = y;
        }
    }
}

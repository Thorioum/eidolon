package net.thorioum.sound;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static net.thorioum.Eidolon.error;
import static net.thorioum.sound.SoundEffectDatabase.SAMPLE_RATE;

public class Util {
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static int getChannelCount(File audioFile) throws UnsupportedAudioFileException, IOException {
        try (AudioInputStream in = AudioSystem.getAudioInputStream(audioFile)) {
            AudioFormat fmt = in.getFormat();
            return fmt.getChannels();
        }
    }

    private static final Pattern DISALLOWED = Pattern.compile("[^a-z0-9_.-]");
    public static String filterString(String input) {
        if (input == null) return null;
        String s = input.toLowerCase(Locale.ROOT).replace(' ', '.');
        return DISALLOWED.matcher(s).replaceAll("");
    }

    public static String formatTime(long milliseconds) {
        long temp = Math.abs(milliseconds);
        temp /= 1000;
        long seconds = temp % 60;
        temp /= 60;
        long minutes = temp % 60;
        temp /= 60;
        long hours = temp;
        StringBuilder sb = new StringBuilder();
        if (milliseconds < 0) {
            sb.append("-");
        }
        if (hours > 0) {
            sb.append(String.format("%d:", hours));
            sb.append(String.format("%02d:", minutes));
        } else {
            sb.append(String.format("%d:", minutes));
        }
        sb.append(String.format("%02d", seconds));
        return sb.toString();
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


    public static void downloadResource(String hash, File destination) {
        try {
            URL url = new URL("https://resources.download.minecraft.net/" + hash.substring(0,2) + "/" + hash);
            try (InputStream in = url.openStream()) {
                Files.copy(in, Paths.get(destination.toURI()), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static JsonObject getResourceJson(String hash) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://resources.download.minecraft.net/" + hash.substring(0,2) + "/" + hash))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " for " + hash);
        }
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }

    public static JsonObject getJson(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        }
        return JsonParser.parseString(resp.body()).getAsJsonObject();
    }
}

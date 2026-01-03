package net.thorioum.matchers;

import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundEffectDatabase;
import net.thorioum.result.SingleSoundResult;
import net.thorioum.sound.SoundMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GreedySubCpuMatcher implements Matcher {

    private int frameSize;
    private int numCandidates;

    private float[] candidateMatrix;
    private float[] norms;
    private String[] names;
    private double[] pitches;

    private boolean ready = false;

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void buildFromDatabase(SoundEffectDatabase db, int frameSize, List<String> blacklistedSounds) {
        this.frameSize = frameSize;

        List<float[]> rows = new ArrayList<>();
        List<Float> normList = new ArrayList<>();
        List<String> nameList = new ArrayList<>();
        List<Double> pitchList = new ArrayList<>();

        for (Map.Entry<String, Map<Double, double[]>> e : db.pitchShiftedEffects.entrySet()) {
            String name = e.getKey();
            if(blacklistedSounds.contains(name)) continue;

            Map<Double, double[]> pitches = e.getValue();
            Map<Double, Double> normMap = db.effectNorms.get(name);
            if (normMap == null) continue;

            for (Map.Entry<Double, double[]> p : pitches.entrySet()) {
                double pitch = p.getKey();
                double[] hp = p.getValue();
                if (hp == null || hp.length != frameSize) continue;
                Double n = normMap.get(pitch);
                if (n == null || n <= 0) continue;

                float[] row = new float[frameSize];
                for (int i = 0; i < frameSize; i++) {
                    row[i] = (float) hp[i];
                }
                rows.add(row);
                normList.add((float) (double) n);
                nameList.add(name);
                pitchList.add(pitch);
            }
        }

        this.numCandidates = rows.size();
        if (numCandidates == 0) {
            ready = false;
            return;
        }

        this.candidateMatrix = new float[numCandidates * frameSize];
        for (int c = 0; c < numCandidates; c++) {
            System.arraycopy(rows.get(c), 0, candidateMatrix, c * frameSize, frameSize);
        }

        this.norms = new float[numCandidates];
        for (int i = 0; i < numCandidates; i++) {
            norms[i] = normList.get(i);
        }

        this.names = nameList.toArray(new String[0]);
        this.pitches = new double[pitchList.size()];
        for (int i = 0; i < pitchList.size(); i++) {
            pitches[i] = pitchList.get(i);
        }

        ready = true;
    }

    @Override
    public SingleSoundResult findBestMatch(ConverterContext ctx, float[] residual, double residualEnergy, float minSim) {
        if (!ready) return null;

        float residualNorm = (float) Math.sqrt(residualEnergy);
        if (residualNorm <= 1e-20f) return null;

        float bestSim = minSim;
        int bestIdx = -1;
        float bestVol = 0.0f;

        int offset = 0;
        for (int c = 0; c < numCandidates; c++) {
            float dot = 0.0f;
            int end = offset + frameSize;
            for (int i = offset; i < end; i++) {
                dot += candidateMatrix[i] * residual[i - offset];
            }
            offset = end;

            float n = norms[c];
            if (n <= 1e-20f) continue;

            float cs = dot / (n * residualNorm);
            if (cs < 0.0f) cs = 0.0f;
            float sim = cs * n;

            if (sim <= bestSim) continue;

            float vol = dot / (n * n + 1e-10f);
            if (vol < 0.0f) vol = 0.0f;
            if (vol > 1.0f) vol = 1.0f;

            bestSim = sim;
            bestIdx = c;
            bestVol = vol;
        }

        if (bestIdx < 0) return null;

        String name = names[bestIdx];
        double pitch = pitches[bestIdx];
        return new SingleSoundResult(name, pitch, bestVol, bestSim, SoundMatcher.getDatabase(ctx).pitchShiftedEffects.get(name).get(pitch));
    }
}

package net.thorioum.result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SingleFrameResult {
    public final int frame;
    private final List<SingleSoundResult> composition = new ArrayList<>();
    public SingleFrameResult(int frame) {
        this.frame = frame;
    }
    public void addEffect(SingleSoundResult match) {
        synchronized (composition) {
            composition.add(match);
            composition.sort(Comparator.comparingDouble(o -> 1.0/o.volume()));
        }
    }
    public List<SingleSoundResult> getComposition() {
        return new ArrayList<>(composition);
    }
}

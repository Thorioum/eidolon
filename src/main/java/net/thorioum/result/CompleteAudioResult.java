package net.thorioum.result;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CompleteAudioResult {
    private final List<SingleFrameResult> compositions = new ArrayList<>();

    public int expectedFrames = -1;
    public void addFrame(SingleFrameResult match) {
        synchronized (compositions) {
            compositions.add(match);
            compositions.sort(Comparator.comparingInt(o -> o.frame));
        }
    }
    public boolean complete() {
        return compositions.size() >= expectedFrames;
    }
    public List<SingleFrameResult> composition() {
        return new ArrayList<>(compositions);
    }
}

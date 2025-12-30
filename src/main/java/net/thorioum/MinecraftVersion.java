package net.thorioum;

public class MinecraftVersion implements Comparable<MinecraftVersion> {
    private int major, minor, patch;
    private Stage stage;
    private int stageNumber = 0;

    private String versionString;
    public MinecraftVersion(String versionString) {
        this.versionString = versionString;
    }

    enum Stage {
        SNAPSHOT, PRE, RC, RELEASE
    }

    public String str() {
        return versionString;
    }

    public boolean isAfter(MinecraftVersion other) {
        return this.compareTo(other) > 0;
    }

    public static MinecraftVersion parse(String s) {
        MinecraftVersion v = new MinecraftVersion(s);

        if (s.matches("\\d{2}w\\d{2}[a-z]")) {
            v.stage = Stage.SNAPSHOT;
            return v;
        }

        String[] baseAndSuffix = s.split("-", 2);
        String[] nums = baseAndSuffix[0].split("\\.");

        v.major = parseInt(nums, 0);
        v.minor = parseInt(nums, 1);
        v.patch = parseInt(nums, 2);

        if (baseAndSuffix.length == 1) {
            v.stage = Stage.RELEASE;
            return v;
        }

        String suffix = baseAndSuffix[1];

        if (suffix.startsWith("pre")) {
            v.stage = Stage.PRE;
            v.stageNumber = Integer.parseInt(suffix.substring(3));
        } else if (suffix.startsWith("rc")) {
            v.stage = Stage.RC;
            v.stageNumber = Integer.parseInt(suffix.substring(2));
        } else {
            v.stage = Stage.RELEASE;
        }

        return v;
    }

    private static int parseInt(String[] a, int i) {
        return i < a.length ? Integer.parseInt(a[i]) : 0;
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        int c;

        if ((c = Integer.compare(major, o.major)) != 0) return c;
        if ((c = Integer.compare(minor, o.minor)) != 0) return c;
        if ((c = Integer.compare(patch, o.patch)) != 0) return c;

        if ((c = Integer.compare(stageRank(), o.stageRank())) != 0) return c;

        return Integer.compare(stageNumber, o.stageNumber);
    }
    @Override
    public String toString() {
        return "MinecraftVersion[" + versionString + "]";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftVersion that = (MinecraftVersion) o;
        return that.compareTo(this) == 0;
    }
    @Override
    public int hashCode() {
        return versionString != null ? versionString.hashCode() : 0;
    }
    private int stageRank() {
        return switch (stage) {
            case SNAPSHOT -> 0;
            case PRE -> 1;
            case RC -> 2;
            case RELEASE -> 3;
        };
    }
}

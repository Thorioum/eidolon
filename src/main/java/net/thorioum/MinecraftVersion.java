package net.thorioum;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import static net.thorioum.Eidolon.error;

public class MinecraftVersion implements Comparable<MinecraftVersion> {

    private static final Map<String, Long> versionDateCache = new HashMap<>();
    private final String versionString;
    private long releaseDate = -1;

    public MinecraftVersion(String versionString, String releaseDateString) {
        this.versionString = versionString;
        try {
            this.releaseDate = OffsetDateTime.parse(releaseDateString).toEpochSecond();
            versionDateCache.put(versionString,releaseDate);
        } catch (DateTimeParseException e) {
            error(e.getMessage());
            e.printStackTrace();
        }
    }
    public MinecraftVersion(String versionString, long time) {
        this.versionString = versionString;
        this.releaseDate = time;
    }

    public String str() {
        return versionString;
    }

    public boolean isAfter(MinecraftVersion other) {
        return this.compareTo(other) > 0;
    }

    public static MinecraftVersion get(String s) {
        long time = versionDateCache.getOrDefault(s,-1L);
        return new MinecraftVersion(s,time);
    }

    private static int parseInt(String[] a, int i) {
        return i < a.length ? Integer.parseInt(a[i]) : 0;
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        return Long.compare(this.releaseDate,o.releaseDate);
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
}

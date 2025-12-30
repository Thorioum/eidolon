package net.thorioum.sound;

import com.google.gson.*;
import net.thorioum.MinecraftVersion;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static net.thorioum.Eidolon.error;
import static net.thorioum.Eidolon.info;

public class SoundFilesGrabber {
    public record SoundFilesEntry(
            Map<String, Float> soundVolumesMap,
            Map<String, Float> soundPitchesMap,
            Map<String, File> soundFilesMap){}

    public static final Map<MinecraftVersion, SoundFilesEntry> soundMap = new ConcurrentHashMap<>();

    public static boolean allowsDynamicTickrate(MinecraftVersion version) {
        MinecraftVersion firstVersionWithTick = MinecraftVersion.parse("23w43a");
        return version.isAfter(firstVersionWithTick);
    }

    public static void init() {
        info("Resolving minecraft versions sounds. . .");

        Path mcDir = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", ".minecraft");

        Path versionsDir = mcDir.resolve("versions");

        Set<Path> versionDirs = new HashSet<>();
        try (Stream<Path> pathStream = Files.walk(versionsDir, 1)) {
            pathStream
                    .filter(Files::isDirectory)
                    .filter(path -> !versionsDir.equals(path))
                    .filter(p -> p.getFileName()
                            .toString()
                            .matches("^\\d.*"))
                    .forEach(versionDirs::add);

        } catch (IOException e) {
            error(e.getMessage());
            e.printStackTrace();
        }

        for(Path versionDir : versionDirs) {
            String versionName = versionDir.getFileName().toString();
            try {
                MinecraftVersion.parse(versionName);
            } catch (NumberFormatException e) {
                continue;
            }

            final Map<String, Float> volumes = new ConcurrentHashMap<>();
            final Map<String, Float> pitches = new ConcurrentHashMap<>();
            final Map<String, File> soundFiles = new ConcurrentHashMap<>();

            File versionJsonFile = versionDir.resolve(versionName + ".json").toFile();

            File assetsFile = null;
            try (FileReader reader = new FileReader(versionJsonFile)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                String assetsName = jsonObject.get("assets").getAsString();

                Path assetsPath = mcDir.resolve("assets/indexes/" + assetsName + ".json");
                assetsFile = assetsPath.toFile();
            } catch (Exception ignored) {

            }

            if(assetsFile == null || !assetsFile.exists()) continue;

            String soundsJsonHash = null;
            JsonObject assetsObject = null;
            try (FileReader reader = new FileReader(assetsFile)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                JsonObject soundsJsonObject = jsonObject.getAsJsonObject("objects").getAsJsonObject("minecraft/sounds.json");
                soundsJsonHash = soundsJsonObject.get("hash").getAsString();
                assetsObject = jsonObject;
            } catch (Exception e) {
                error(assetsFile + " : " + e.getMessage());
                e.printStackTrace();
            }

            if(soundsJsonHash == null) continue;

            File soundsJsonFile = mcDir.resolve("assets/objects/" + soundsJsonHash.substring(0,2) + "/" + soundsJsonHash).toFile();

            try (FileReader reader = new FileReader(soundsJsonFile)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                for(String sound : jsonObject.keySet()) {
                    JsonArray possibleSubSounds = jsonObject.getAsJsonObject(sound).getAsJsonArray("sounds");

                    //ignore sound effects that could possibly play different sound files
                    if(possibleSubSounds.size() != 1) continue;

                    JsonElement soundElement = possibleSubSounds.get(0);
                    final float volume;
                    final float pitch;
                    String soundFileName;
                    if(soundElement instanceof JsonObject soundObject) {
                        volume = soundObject.has("volume") ? soundObject.get("volume").getAsFloat() : 1.0f;
                        pitch = soundObject.has("pitch") ? soundObject.get("pitch").getAsFloat() : 1.0f;
                        soundFileName = soundObject.get("name").getAsString();
                    } else {
                        volume = 1.0f;
                        pitch = 1.0f;
                        soundFileName = soundElement.getAsString();
                    }

                    volumes.put(sound, volume);
                    pitches.put(sound, pitch);
                    soundFileName = "minecraft/sounds/" + soundFileName + ".ogg";

                    try {
                        String soundFileHash = assetsObject
                                .getAsJsonObject("objects")
                                .getAsJsonObject(soundFileName)
                                .get("hash").getAsString();
                        File soundFile = mcDir.resolve("assets/objects/" + soundFileHash.substring(0,2) + "/" + soundFileHash).toFile();
                        soundFiles.put(sound,soundFile);
                    } catch (Exception ignored) {
                    }
                }

            } catch (Exception e) {
                error(e.getMessage());
                e.printStackTrace();
            }

            soundMap.put(MinecraftVersion.parse(versionName),new SoundFilesEntry(volumes,pitches,soundFiles));
        }

        info("Sounds Index Initialized");

    }

}

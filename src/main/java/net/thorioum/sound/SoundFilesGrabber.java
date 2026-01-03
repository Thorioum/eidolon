package net.thorioum.sound;

import com.google.gson.*;
import net.thorioum.MinecraftVersion;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static net.thorioum.Eidolon.error;
import static net.thorioum.Eidolon.info;
import static net.thorioum.sound.Util.*;

public class SoundFilesGrabber {

    public static class SoundDataEntries{
        private final MinecraftVersion version;
        private final String url;
        private boolean resolved = false;
        private final Map<String, File> fileMap = new HashMap<>();

        public SoundDataEntries(MinecraftVersion version, String versionUrl) {
            this.version = version;
            this.url = versionUrl;
        }

        public Map<String, File> fileMap() {
            Path cacheDir = Paths.get("sounds",version.str());
            cacheDir.toFile().mkdirs();

            return fileMap;
        }
        public boolean isResolved() {
            return resolved;
        }
        private boolean resolving = false;
        public void resolveSounds() {
            if(isResolved() || resolving) return;
            resolving = true;
            try {
                JsonObject versionJson = getJson(url);

                JsonObject assetIndex = versionJson.getAsJsonObject("assetIndex");
                String versionAssetsUrl = assetIndex.get("url").getAsString();

                JsonObject versionAssetsJson = getJson(versionAssetsUrl).getAsJsonObject("objects");
                JsonObject soundsJsonObject = versionAssetsJson.getAsJsonObject("minecraft/sounds.json");
                String soundsJsonHash = soundsJsonObject.get("hash").getAsString();

                final Map<String, Float> volumes = soundMap.get(version).soundVolumesMap;
                final Map<String, Float> pitches = soundMap.get(version).soundPitchesMap;
                fileMap();

                JsonObject soundsJson = getResourceJson(soundsJsonHash);
                for (String sound : soundsJson.keySet()) {
                    File destinationFile = Paths.get("sounds",version.str(),sound + ".ogg").toFile();
                    File soundConfigFile = Paths.get("sounds",version.str(),sound + ".json").toFile();

                    if(destinationFile.exists() && soundConfigFile.exists()) {
                        fileMap().put(sound, destinationFile);

                        try (BufferedReader reader = Files.newBufferedReader(soundConfigFile.toPath())) {
                            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                            volumes.put(sound, config.get("volume").getAsFloat());
                            pitches.put(sound, config.get("pitch").getAsFloat());
                        } catch (Exception e) {
                            error(e.getMessage());
                            e.printStackTrace();
                        }
                        continue;
                    }


                    JsonArray possibleSubSounds = soundsJson.getAsJsonObject(sound).getAsJsonArray("sounds");

                    //ignore sound effects that could possibly play different sound files
                    if (possibleSubSounds.size() != 1) continue;

                    JsonElement soundElement = possibleSubSounds.get(0);
                    final float volume;
                    final float pitch;
                    String soundFileName;
                    if (soundElement instanceof JsonObject soundObject) {
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
                        String soundFileHash = versionAssetsJson
                                .getAsJsonObject(soundFileName)
                                .get("hash").getAsString();

                        downloadResource(soundFileHash,destinationFile);
                        fileMap().put(sound,destinationFile);

                        JsonObject config = new JsonObject();
                        config.addProperty("volume",volume);
                        config.addProperty("pitch",pitch);

                        try (FileWriter writer = new FileWriter(soundConfigFile)) {
                            new Gson().toJson(config, writer);
                        } catch (IOException e) {
                            error(e.getMessage());
                            e.printStackTrace();
                        }

                    } catch (Exception ignored) {

                    }
                }
                resolved = true;
            } catch (Exception e) {
                error(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public record SoundFilesEntry(
            Map<String, Float> soundVolumesMap,
            Map<String, Float> soundPitchesMap,
            SoundDataEntries soundFilesMap)
    {}

    public static final Map<MinecraftVersion, SoundFilesEntry> soundMap = new ConcurrentHashMap<>();

    public static boolean allowsDynamicTickrate(MinecraftVersion version) {
        MinecraftVersion firstVersionWithTick = new MinecraftVersion("23w43a",1698240877);
        return version.isAfter(firstVersionWithTick) || version.compareTo(firstVersionWithTick) == 0;
    }


    private static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";


    public static void init() {
        info("Resolving minecraft versions sounds. . .");
        Path cacheDir = Paths.get("sounds");
        cacheDir.toFile().mkdirs();

        try {
            JsonObject versionManifest = getJson(VERSION_MANIFEST);
            readManifest(versionManifest);
            File manifest = Paths.get("sounds","manifest.json").toFile();
            if(!manifest.exists()) manifest.createNewFile();

            //cache it
            try (FileWriter writer = new FileWriter(manifest)) {
                new Gson().toJson(versionManifest, writer);
            } catch (IOException e) {
                error(e.getMessage());
                e.printStackTrace();
            }

            info("Resolved version manifest from mojang website.");
            return;
        } catch (Exception e) {
            error(e.getMessage());
            e.printStackTrace();
        }

        if(Files.exists(Paths.get("sounds","manifest.json"))) {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get("sounds","manifest.json"))) {
                JsonObject versionManifest = JsonParser.parseReader(reader).getAsJsonObject();
                readManifest(versionManifest);
            } catch (Exception e) {
                error(e.getMessage());
                e.printStackTrace();
            }
        }

        info("Resolved version manifest from local cache");

    }
    private static void readManifest(JsonObject versionManifest) {
        JsonArray versionsArray = versionManifest.getAsJsonArray("versions");
        for(JsonElement element : versionsArray) {
            if(!(element instanceof JsonObject versionObject)) continue;

            String versionUrl = versionObject.get("url").getAsString();
            String time = versionObject.get("releaseTime").getAsString();
            String id = versionObject.get("id").getAsString();

            MinecraftVersion version = new MinecraftVersion(id,time);

            SoundFilesEntry entry = new SoundFilesEntry(new HashMap<>(),new HashMap<>(),new SoundDataEntries(version,versionUrl));
            soundMap.put(version,entry);
        }
    }


}

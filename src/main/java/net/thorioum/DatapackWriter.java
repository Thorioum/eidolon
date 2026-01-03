package net.thorioum;

import net.thorioum.result.CompleteAudioResult;
import net.thorioum.result.SingleFrameResult;
import net.thorioum.result.SingleSoundResult;
import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundFilesGrabber;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.thorioum.Eidolon.error;

public class DatapackWriter {

    public static void createAudioPack(ConverterContext ctx, File file, CompleteAudioResult result) {
        try {
            String name = file.getName();

            File unzippedFile = new File(file.getParentFile().getAbsolutePath() + "/datatmp" + name);
            unzippedFile.mkdirs();

            File framerateFile = new File(unzippedFile.getAbsolutePath() + "/framerate.txt");
            framerateFile.createNewFile();
            writeFile(framerateFile, String.valueOf(ctx.frameLength()));

            File mcmeta = new File(unzippedFile.getAbsolutePath() + "/pack.mcmeta");
            mcmeta.createNewFile();
            writeFile(mcmeta,"{\"pack\":{\"description\":[\"[https://github.com/Thorioum/eidolon] " + name + "\",\"Audio pack provided graciously and created meticulously by the eidolon project. For more information contact 'thorioum' on discord.\"],\"pack_format\":1}}");

            File dataDir = new File(unzippedFile.getAbsolutePath() + "/data");
            dataDir.mkdirs();


            File dataSubDir = new File(dataDir.getAbsolutePath() + "/" + name);
            dataSubDir.mkdirs();
            dataSubDir = new File(dataSubDir.getAbsolutePath() + "/" + (ctx.version().isAfterOrEqual(SoundFilesGrabber.tryGetVersion("1.21")) ? "function" : "functions"));
            dataSubDir.mkdirs();


            File playFuncFile = new File(dataSubDir.getAbsolutePath() + "/play.mcfunction");
            playFuncFile.createNewFile();
            writeFile(playFuncFile, "schedule function " + name + ":_/0 1t append");

            File frameDir = new File(dataSubDir.getAbsolutePath() + "/_");
            frameDir.mkdirs();

            //create frames
            for(int i = 0; i < result.composition().size() + 1; i++) {
                if(i == result.composition().size()) {
                    String frameStringData = "# https://github.com/Thorioum/eidolon\n\nexecute run stopsound @a[tag=!nomusic,tag=!nm] record\n";
                    File frameFile = new File(frameDir.getAbsolutePath() + "/" + i + ".mcfunction");
                    frameFile.createNewFile();
                    writeFile(frameFile, frameStringData);
                    break;
                }
                SingleFrameResult soundEffectComposition = result.composition().get(i);
                File frameFile = new File(frameDir.getAbsolutePath() + "/" + i + ".mcfunction");
                frameFile.createNewFile();

                String frameStringData = "# https://github.com/Thorioum/eidolon\n\nexecute run stopsound @a[tag=!nomusic,tag=!nm] record\n";
                for(SingleSoundResult match : soundEffectComposition.getComposition()) {
                    String cmd = match.asCommand();
                    if(cmd.startsWith("/")) cmd = cmd.substring(1);
                    frameStringData += cmd + "\n";
                }
                frameStringData += "\nschedule function " + name + ":_/" + (i+1) + " " + (ctx.frameLength() < 50 ? ctx.frameLength()/10 : (ctx.frameLength() / 50)) + "t" + (ctx.version().isAfterOrEqual(SoundFilesGrabber.tryGetVersion("1.15")) ? " append" : "");

                writeFile(frameFile, frameStringData);
            }

            Path zippedFile = Paths.get(file.getParentFile().getAbsolutePath() + "/" + name + ".zip");
            zipDirectoryAndDeleteSource(unzippedFile.toPath(), zippedFile);
            unzippedFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            error(e.getMessage());
        }
    }
    private static void writeFile(File mcmeta, String str) {
        try (FileWriter fileWriter = new FileWriter(mcmeta);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

            bufferedWriter.write(str);

        } catch (IOException e) {
            e.printStackTrace();
            error(e.getMessage());
        }
    }

    public static void zipDirectoryAndDeleteSource(Path sourceDir, Path zipFile) throws IOException {
        Objects.requireNonNull(sourceDir, "sourceDir");
        Objects.requireNonNull(zipFile, "zipFile");

        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("sourceDir must be a directory: " + sourceDir);
        }

        final Path sourceReal = sourceDir.toRealPath(LinkOption.NOFOLLOW_LINKS);
        if (sourceReal.getParent() == null) {
            throw new IllegalArgumentException("Refusing to delete a filesystem root: " + sourceReal);
        }

        final Path zipAbs = zipFile.toAbsolutePath().normalize();
        if (zipAbs.startsWith(sourceReal)) {
            throw new IllegalArgumentException("zipFile must NOT be inside sourceDir when deletion is requested.");
        }

        if (zipAbs.getParent() != null) {
            Files.createDirectories(zipAbs.getParent());
        }

        zipDirectory(sourceReal, zipAbs);

        if (!Files.exists(zipAbs) || Files.size(zipAbs) == 0L) {
            throw new IOException("Zip creation appears to have failed: " + zipAbs);
        }
        try (java.util.zip.ZipFile ignored = new java.util.zip.ZipFile(zipAbs.toFile())) {

        }

        Files.walkFileTree(sourceReal, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    public static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        Objects.requireNonNull(sourceDir, "sourceDir");
        Objects.requireNonNull(zipFile, "zipFile");
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("sourceDir must be a directory: " + sourceDir);
        }
        if (zipFile.getParent() != null) {
            Files.createDirectories(zipFile.getParent());
        }

        final Path normalizedZip = zipFile.toAbsolutePath().normalize();

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (Files.isSymbolicLink(dir)) return FileVisitResult.SKIP_SUBTREE;

                    Path rel = sourceDir.relativize(dir);
                    if (!rel.toString().isEmpty()) {
                        String entryName = rel.toString().replace('\\', '/') + "/";
                        ZipEntry entry = new ZipEntry(entryName);
                        entry.setTime(attrs.lastModifiedTime().toMillis());
                        zos.putNextEntry(entry);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.isSymbolicLink(file)) return FileVisitResult.CONTINUE;
                    if (file.toAbsolutePath().normalize().equals(normalizedZip)) return FileVisitResult.CONTINUE;

                    Path rel = sourceDir.relativize(file);
                    String entryName = rel.toString().replace('\\', '/');
                    ZipEntry entry = new ZipEntry(entryName);
                    entry.setTime(attrs.lastModifiedTime().toMillis());
                    zos.putNextEntry(entry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

}

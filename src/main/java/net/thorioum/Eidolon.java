package net.thorioum;

import javafx.application.Application;
import javafx.stage.Stage;
import net.thorioum.gui.Launcher;
import net.thorioum.result.CompleteAudioResult;
import net.thorioum.sound.ConverterContext;
import net.thorioum.sound.SoundEffectDatabase;
import net.thorioum.sound.SoundFilesGrabber;
import net.thorioum.sound.SoundMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Eidolon extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger("eidolon");

    public static void initialize() {
        SoundFilesGrabber.init();
    }

    @Override
    public void start(Stage stage) throws Exception {

    }

    public static void main(String[] args) {
        initialize();
        Application.launch(Launcher.class, args);
    }




    public static ExecutorService executor = Executors.newFixedThreadPool((int) (Runtime.getRuntime().availableProcessors()/2));
    public static void resetExecutor() {
        executor.shutdownNow();
        executor = Executors.newFixedThreadPool((int) (Runtime.getRuntime().availableProcessors()/2));
    }

    public static void info(String message) {
        LOGGER.info(message);
    }
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }
    public static void error(String message) {
        LOGGER.error(message);
    }
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    private static boolean soundCheck(ConverterContext ctx) {
        if(ctx.sounds() == null) {
            error("Could not find sound file and metadata for this version. Make sure to have a valid .minecraft directory, and a valid installation of your target version");
        }
        if(ctx.frameLength() % 50 != 0 && !SoundFilesGrabber.allowsDynamicTickrate(ctx.version())) {
            error("Because your select version is below 23w43a (1.20.3 snapshot), and /tick doesnt exist yet, your framerate can only be a multiple of 50ms!");
            return true;
        }
        return false;
    }


    public static CompleteAudioResult process(ConverterContext ctx, int soundsPerFrame, File file) {
        if(soundCheck(ctx)) return null;

        SoundMatcher matcher = new SoundMatcher();

        SoundEffectDatabase db = SoundMatcher.getDatabase(ctx);
        db.processSounds(ctx);

        matcher.enableMatchers(ctx);

        matcher.processAudioFile(ctx,soundsPerFrame,file,(result)->{

        });

        return matcher.result;
    }
}

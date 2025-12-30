package net.thorioum.gui;

import static java.nio.charset.StandardCharsets.UTF_8;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Properties;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Launcher extends Application {

    static final String ASSETS_DIR = "/assets/";

    static final String APP_ICON_PATH = Objects.requireNonNull(
            Launcher.class.getResource(ASSETS_DIR + "icons/app-icon.png")
    ).toExternalForm();

    static final String APP_PROPERTIES_PATH = "/application.properties";

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // obtain application properties from pom.xml
        loadApplicationProperties();

        var scene = new Scene(createWelcomePane(), 800, 600);
        scene.getStylesheets().add(ASSETS_DIR + "index.css");

        stage.setScene(scene);
        stage.setTitle(System.getProperty("app.name"));
        stage.getIcons().add(new Image(APP_ICON_PATH));
        stage.setOnCloseRequest(t -> Platform.exit());
        stage.setMaxWidth(1280);
        stage.setMaxHeight(900);

        Platform.runLater(() -> {
            stage.show();
            stage.requestFocus();
        });
    }

    private Pane createWelcomePane() {
        var root = new VBox();
        root.getStyleClass().add("welcome");
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(
                new Label(
                        "Hi, this is the AtlantaFX starter project. Check out the README for a quick start and happy coding.")
        );

        return root;
    }

    private void loadApplicationProperties() {
        try {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(
                    Objects.requireNonNull(getClass().getResourceAsStream(APP_PROPERTIES_PATH)),
                    UTF_8
            ));
            properties.forEach((key, value) -> System.setProperty(
                    String.valueOf(key),
                    String.valueOf(value)
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
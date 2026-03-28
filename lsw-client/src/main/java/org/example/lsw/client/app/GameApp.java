package org.example.lsw.client.app;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point.
 * All scene construction and navigation lives in
 * SceneManager and the individual ___Scene classes for separation of concerns
 */
public class GameApp extends Application {
    @Override
    public void start(Stage stage) {
        SceneManager sm = new SceneManager(stage);
        sm.showLogin();
    }
    public static void main(String[] args) { launch(args); }
}

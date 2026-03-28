package org.example.lsw.client.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Login screen - the first thing the player sees!
 * Layout:
 *   - Card with game title
 *   - Username + Password fields
 *   - "Login" button  (validates against existing accounts)
 *   - "Create Account" button  (registers a new account)
 *   - Error label shown on failure
 */
public class LoginScene {
    private final VBox root;

    public LoginScene(SceneManager sceneManager) {
        // -----------------------------------------------------------------------
        // |                                Title                                |
        // -----------------------------------------------------------------------
        Label title = new Label("Legends of\nSword and Wand");
        title.setTextAlignment(TextAlignment.CENTER);
        title.setFont(Font.font("Serif", FontWeight.BOLD, 28));
        title.setStyle("-fx-text-fill: #f5f5f5;");

        Label subtitle = new Label("Sign in to continue your adventure");
        subtitle.setFont(Font.font("Serif", 14));
        subtitle.setStyle("-fx-text-fill: #888888;");

        // -----------------------------------------------------------------------
        // |                               Fields                                |
        // -----------------------------------------------------------------------
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(320);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(320);

        // -----------------------------------------------------------------------
        // |                           Error label                                |
        // -----------------------------------------------------------------------
        //Error label (hidden until needed) =================================
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #cc3333; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // -----------------------------------------------------------------------
        // |                              Buttons                                |
        // -----------------------------------------------------------------------
        Button loginBtn = new Button("Login");
        loginBtn.setPrefWidth(150);
        loginBtn.setDefaultButton(true);
        loginBtn.setStyle("""
            -fx-background-color: #3a7bd5; -fx-text-fill: white;
            -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;
        """);

        Button createBtn = new Button("Create Account");
        createBtn.setPrefWidth(150);
        createBtn.setStyle("""
            -fx-background-color: #5a5a5a; -fx-text-fill: white;
            -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;
        """);

        HBox buttonRow = new HBox(12, loginBtn, createBtn);
        buttonRow.setAlignment(Pos.CENTER);

        loginBtn.setOnAction(e -> {
            String err = sceneManager.login(usernameField.getText(), passwordField.getText());
            if (err != null) { errorLabel.setText(err); errorLabel.setVisible(true); }
            else sceneManager.showMainMenu();
        });

        createBtn.setOnAction(e -> {
            String err = sceneManager.createAccount(usernameField.getText(), passwordField.getText());
            if (err != null) { errorLabel.setText(err); errorLabel.setVisible(true); }
            else sceneManager.showMainMenu();
        });

        // -----------------------------------------------------------------------
        // |                                Card                                 |
        // -----------------------------------------------------------------------
        VBox card = new VBox(16, title, subtitle, usernameField, passwordField, errorLabel, buttonRow);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(420);
        card.setStyle("""
            -fx-background-color: #2b2b2b; -fx-background-radius: 12;
            -fx-border-color: #444444; -fx-border-radius: 12; -fx-border-width: 1;
        """);

        // -----------------------------------------------------------------------
        // |                                Root                                 |
        // -----------------------------------------------------------------------
        root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a1a;");
    }

    public VBox getRoot() { return root; }
}

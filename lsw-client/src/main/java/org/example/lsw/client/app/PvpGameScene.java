package org.example.lsw.client.app;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.example.lsw.client.api.ApiClient;
import org.example.lsw.client.api.ApiException;

/**
 * Battle screen for PvP matches.
 * Looks just like GameScenes layout (console + input field) but runs pvp
 * instead of pve. When the battle ends it:
 *   1) Records the win/loss on both user profiles
 *   2)Saves both parties back to their owners PvP roster
 *   3) Navigates to PvpResultsScene
 */
public class PvpGameScene {
    private final VBox root;
    private final TextArea console;
    private final TextField inputField;
    private final Button submitBtn;

    private final SceneManager sceneManager;
    private final ApiClient api;
    private final String matchId;
    private final String battleId;

    public PvpGameScene(SceneManager sceneManager,
                        String matchId,
                        String battleId,
                        String p1Username,
                        String p1PartyName,
                        String p2Username,
                        String p2PartyName) {
        this.sceneManager = sceneManager;
        this.api = sceneManager.getApi();
        this.matchId = matchId;
        this.battleId = battleId;

        // -----------------------------------------------------------------------
        // |                              Top bar                                |
        // -----------------------------------------------------------------------
        Label header = new Label("⚔  " + p1Username + " [" + p1PartyName + "]"
                + "   vs   "
                + p2Username + " [" + p2PartyName + "]");
        header.setFont(Font.font("Serif", 15));
        header.setStyle("-fx-text-fill: #cccccc;");

        HBox topBar = new HBox(header);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 8, 16));
        topBar.setStyle("-fx-background-color: #1e1e3a; -fx-border-color: #333355; -fx-border-width: 0 0 1 0;");

        // -----------------------------------------------------------------------
        // |                              Console                                |
        // -----------------------------------------------------------------------
        console = new TextArea();
        console.setEditable(false);
        console.setWrapText(true);
        console.setFont(Font.font("Monospaced", 13));
        console.setStyle("""
            -fx-control-inner-background: #1a1a1a;
            -fx-text-fill: #d0d0d0;
            -fx-border-color: transparent;
        """);
        VBox.setVgrow(console, Priority.ALWAYS);

        // -----------------------------------------------------------------------
        // |                            Input row                                |
        // -----------------------------------------------------------------------
        inputField = new TextField();
        inputField.setPromptText("Type a battle command and press Enter…");
        inputField.setFont(Font.font("Monospaced", 13));
        inputField.setStyle("""
            -fx-background-color: #2a2a2a;
            -fx-text-fill: #e0e0e0;
            -fx-prompt-text-fill: #555555;
            -fx-border-color: #444444;
            -fx-border-radius: 5;
            -fx-background-radius: 5;
        """);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        submitBtn = new Button("Submit");
        submitBtn.setStyle("""
            -fx-background-color: #7b2020;
            -fx-text-fill: white;
            -fx-background-radius: 5;
            -fx-cursor: hand;
        """);

        HBox bottomBar = new HBox(8, inputField, submitBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 16, 10, 16));
        bottomBar.setStyle("-fx-background-color: #1e1e2e;");

        //submit handler
        Runnable submit = () -> {
            String line = inputField.getText().trim();
            if (line.isBlank()) return;
            inputField.clear();
            appendLine("> " + line);
            handleInput(line);
        };

        submitBtn.setOnAction(e -> submit.run());
        inputField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) submit.run(); });

        root = new VBox(topBar, console, bottomBar);
        root.setStyle("-fx-background-color: #1a1a1a;");
    }

    /**
     * start a pvp match on a separate thread
     */
    public void startMatch() {
        //load initial battle state (opening messages + first turn prompt)
        new Thread(() -> {
            try {
                JsonNode state = api.getBattleState(battleId);
                Platform.runLater(() -> displayBattleState(state));
            } catch (ApiException e) {
                Platform.runLater(() -> appendLine("Error loading battle: " + e.getMessage()));
            }
        }, "pvp-init").start();
    }

    /**
     * Handle pvp inputs
     */
    private void handleInput(String line) {
        setInputEnabled(false);
        new Thread(() -> {
            try {
                String[] parts = line.trim().split("\\s+", 3);
                String action = parts[0].toLowerCase();

                JsonNode state = switch (action) {
                    case "attack" -> {
                        if (parts.length < 2) throw new ApiException("Usage: attack <target>");
                        yield api.battleAction(battleId, "attack", parts[1], null);
                    }
                    case "defend" -> api.battleAction(battleId, "defend", null, null);
                    case "wait"   -> api.battleAction(battleId, "wait",   null, null);
                    case "cast" -> {
                        if (parts.length < 2) throw new ApiException("Usage: cast \"<ability>\" <target>");
                        String ability = parts[1].replaceAll("\"", "");
                        String target  = parts.length > 2 ? parts[2] : null;
                        yield api.battleAction(battleId, "cast", target, ability);
                    }
                    default -> throw new ApiException("Unknown command: " + action + "\nValid: attack <target> | defend | wait | cast \"<ability>\" <target>");
                };

                //display the battle state
                Platform.runLater(() -> {
                    displayBattleState(state);
                    if (state.has("finished") && state.get("finished").asBoolean()) {
                        onBattleFinished();
                    } else {
                        setInputEnabled(true);
                    }
                });

            } catch (ApiException e) {
                Platform.runLater(() -> {
                    appendLine("Error: " + e.getMessage());
                    setInputEnabled(true);
                });
            }
        }, "pvp-action").start();
    }

    /**
     * Print message list into console
     */
    private void displayBattleState(JsonNode state) {
        JsonNode messages = state.get("messages");
        if (messages != null && messages.isArray())
            for (JsonNode msg : messages) appendLine(msg.asText());
    }

    //pvp battle finished
    private void onBattleFinished() {
        appendLine("\n--- Battle ended. Resolving match... ---");
        setInputEnabled(false);

        new Thread(() -> {
            try {
                JsonNode result = api.resolvePvpMatch(matchId);
                Platform.runLater(() -> {
                    String winner = result.has("winnerUsername") ? result.get("winnerUsername").asText() : "Unknown";
                    String p1User = result.has("player1Username") ? result.get("player1Username").asText() : "";
                    String p2User = result.has("player2Username") ? result.get("player2Username").asText() : "";
                    String p1Party = result.has("player1PartyName") ? result.get("player1PartyName").asText() : "";
                    String p2Party = result.has("player2PartyName") ? result.get("player2PartyName").asText() : "";

                    //create a new pvp results scene using the aftermath data from the pvp battle
                    PvpResultsScene results = new PvpResultsScene(
                            sceneManager, winner,
                            p1User, p1Party,
                            p2User, p2Party
                    );

                    //set the scene to it
                    Stage stage = (Stage) root.getScene().getWindow();
                    stage.setScene(new Scene(results.getRoot(), root.getScene().getWidth(), root.getScene().getHeight()));
                });
            } catch (ApiException e) {
                Platform.runLater(() -> appendLine("Error resolving match: " + e.getMessage()));
            }
        }, "pvp-resolve").start();
    }

    private void appendLine(String line) {console.appendText(line + "\n");}
    private void setInputEnabled(boolean on) {inputField.setDisable(!on); submitBtn.setDisable(!on);}
    public VBox getRoot() {return root;}
}

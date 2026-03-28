package org.example.lsw.client.app;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import org.example.lsw.client.api.ApiClient;
import org.example.lsw.client.api.ApiException;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;

/**
 * The main game screen.
 * Layout:
 *   TOP    - Campaign title bar
 *   CENTER - TextArea (console)
 *   BOTTOM - TextField + Submit button
 */
public class GameScene {
    private final VBox root;
    private final TextArea console;
    private final TextField inputField;
    private final Button submitBtn;
    private final Label campaignLabel;
    private final SceneManager sceneManager;
    private final ApiClient api;
    private final String sessionId;
    private final String campaignName;

    //tracks whether we're currently in a battle sub-session
    private String activeBattleId = null;

    //tracks current room and party for saving progress on quit
    private int currentRoom = 0;
    private String partyName = "";
    private Party currentParty = null;

    public GameScene(SceneManager sceneManager, String sessionId, String campaignName) {
        this.sceneManager = sceneManager;
        this.api = sceneManager.getApi();
        this.sessionId = sessionId;
        this.campaignName = campaignName;

        // -----------------------------------------------------------------------
        // |                                Top bar                              |
        // -----------------------------------------------------------------------
        campaignLabel = new Label(campaignName + " — Room 0 / 30");
        campaignLabel.setFont(Font.font("Serif", 15));
        campaignLabel.setStyle("-fx-text-fill: #cccccc;");

        HBox topBar = new HBox(campaignLabel);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 8, 16));
        topBar.setStyle("""
             -fx-background-color: #1e1e3a;
             -fx-border-color: #333355;
             -fx-border-width: 0 0 1 0;
        """);

        // -----------------------------------------------------------------------
        // |                                Console                              |
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
        inputField.setPromptText("Type a command and press Enter…");
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
            -fx-background-color: #3a7bd5;
            -fx-text-fill: white;
            -fx-background-radius: 5;
            -fx-cursor: hand;
        """);

        // "Back to Menu" - only visible after campaign ends
        Button backBtn = new Button("< Back to Menu");
        backBtn.setVisible(false);
        backBtn.setManaged(false);
        backBtn.setStyle("""
            -fx-background-color: #555555;
            -fx-text-fill: white;
            -fx-background-radius: 5;
            -fx-cursor: hand;
        """);
        backBtn.setOnAction(e -> sceneManager.showMainMenu());

        HBox bottomBar = new HBox(8, inputField, submitBtn, backBtn);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 16, 10, 16));
        bottomBar.setStyle("-fx-background-color: #494033;");

        // -----------------------------------------------------------------------
        // |                          Submit Handler                             |
        // -----------------------------------------------------------------------
        Runnable submit = () -> {
            String line = inputField.getText().trim();
            if (line.isBlank()) return;
            inputField.clear();
            appendLine("> " + line);
            handleInput(line, backBtn);
        };
        submitBtn.setOnAction(e -> submit.run());
        inputField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) submit.run(); });

        // -----------------------------------------------------------------------
        // |                                Root                                 |
        // -----------------------------------------------------------------------
        root = new VBox(topBar, console, bottomBar);
        root.setStyle("-fx-background-color: #1a1a1a;");
    }

    /**
     * Loads initial state from the server after the scene is shown
     * and runs on a background thread to avoid blocking the JavaFX thread
     */
    public void startGame() {
        new Thread(() -> {
            try {
                JsonNode state = api.getCampaignState(sessionId);
                Platform.runLater(() -> displayState(state));
            } catch (ApiException e) {
                Platform.runLater(() -> appendLine("Error loading campaign: " + e.getMessage()));
            }
        }, "campaign-init").start();
    }

    /**
     * Routes the player's text input to the correct service based on current phase of the campaign
     */
    private void handleInput(String line, Button backBtn) {
        setInputEnabled(false);

        new Thread(() -> {
            try {
                JsonNode state;
                String trimmedLine = line.trim().toLowerCase();

                if (trimmedLine.equals("resolve-battle") || trimmedLine.equals("next") || trimmedLine.equals("quit")) {
                    //Campaign commands: always go to campaign-service
                    activeBattleId = null;
                    state = parseCampaignAction(line.trim());
                } else if (activeBattleId != null) {
                    //Battle phase: player acts, then automatically process enemy turns
                    JsonNode playerActionState = handleBattleInput(line);

                    //print the player's action messages immediately
                    JsonNode playerMsgs = playerActionState.get("messages");
                    if (playerMsgs != null && playerMsgs.isArray()) {
                        for (JsonNode msg : playerMsgs) {
                            final String text = msg.asText();
                            Platform.runLater(() -> appendLine(text));
                        }
                    }

                    //Process enemy AI inputs
                    state = processEnemyTurns(playerActionState);

                    //Auto-resolve battle if it just ended (fixed from earlier version, where we had to manually type this in)
                    if (state.has("finished") && state.get("finished").asBoolean()) {
                        activeBattleId = null;
                        state = parseCampaignAction("resolve-battle");
                    }
                } else {
                    //overworld / inn phase: route to campaign-service
                    state = parseCampaignAction(line);
                }

                //if entering a battle for the first time, get the initial state
                //synchronously here (on the background thread) so it's ready to display
                //in the correct order before input is re-enabled for the player.
                final JsonNode finalState = state;
                JsonNode battleInitState = null;
                String newPhase = finalState.has("phase") ? finalState.get("phase").asText() : "";
                if ("BATTLE".equals(newPhase) && activeBattleId == null) {
                    //extract the battleId from the campaign response
                    String newBattleId = finalState.has("activeBattleSessionId")
                            && !finalState.get("activeBattleSessionId").isNull()
                            ? finalState.get("activeBattleSessionId").asText() : null;
                    if (newBattleId == null) {
                        for (JsonNode msg : finalState.get("messages")) {
                            String txt = msg.asText();
                            if (txt.contains("battleId:")) {
                                newBattleId = txt.substring(txt.indexOf("battleId:") + 9).trim();
                                if (newBattleId.contains(" "))
                                    newBattleId = newBattleId.substring(0, newBattleId.indexOf(" "));
                                break;
                            }
                        }
                    }
                    if (newBattleId != null) {
                        try {battleInitState = api.getBattleState(newBattleId);}
                        catch (ApiException ignored) {}
                    }
                }

                final JsonNode finalBattleInitState = battleInitState;
                Platform.runLater(() -> {
                    if (finalBattleInitState != null) {
                        //just entered a battle. Campaign messages then initial state
                        displayState(finalState);
                        displayBattleState(finalBattleInitState);
                    } else if (activeBattleId == null) {
                        //overworld/inn/resolve. Display normally.
                        displayState(finalState);
                    }
                    //if activeBattleId != null, all messages already printed inline above

                    //quit - save progress then navigate back to main menu
                    if (trimmedLine.equals("quit")) {
                        if (currentParty != null) {
                            sceneManager.saveCampaignProgress(campaignName, partyName, currentRoom, currentParty);
                        }
                        sceneManager.showMainMenu();
                        return;
                    }

                    setInputEnabled(true);

                    // Check if campaign fully completed (all 30 rooms done)
                    String campaignPhase = finalState.has("phase") ? finalState.get("phase").asText() : "";
                    boolean campaignDone = "FINISHED".equals(campaignPhase) ||
                            (finalState.has("finished") &&
                             finalState.get("finished").asBoolean() &&
                             !finalState.has("partyA"));
                    if (campaignDone) {
                        setInputEnabled(false);
                        if (finalState.has("playerParty")) {
                            try {
                                Party party = GameMapper.fromJson(finalState.get("playerParty").toString(), Party.class);
                                sceneManager.showCampaignComplete(sessionId, party);
                            } catch (Exception ignored) {
                                sceneManager.showMainMenu();
                            }
                        }
                        else {
                            sceneManager.showMainMenu();
                        }
                    }
                });

            } catch (ApiException e) {
                Platform.runLater(() -> {
                    appendLine("Error: " + e.getMessage());
                    setInputEnabled(true);
                });
            }
        }, "campaign-action").start();
    }

    /**
     * The server's StatefulBattleEngine runs the enemy AI
     * and returns the result with fully formatted messages.
     * We keep going until it's a player unit's turn or the battle ends.
     */
    private JsonNode processEnemyTurns(JsonNode state) {
        int safetyLimit = 50; //max limit for how many AI turns to process. Fixes the infinite loop glitch (look in the gc. Mar 27)
        int count = 0;

        while (count++ < safetyLimit) {
            //process enemy units unless the battle is finished, or it's the player's turn
            if (state.has("finished") && state.get("finished").asBoolean()) break;
            if (!state.has("currentTurnUnitName")) break;

            //current unit
            String currentUnit = state.get("currentTurnUnitName").asText();

            //if it's a player unit's turn, stop.
            JsonNode partyA = state.get("partyA");
            if (isPlayerUnit(currentUnit, partyA)) break;

            //it's an enemy turn. Send "ai" so the server runs the enemy AI
            try {
                JsonNode newState = api.battleAction(activeBattleId, "ai", null, null);
                state = newState;

                //always print this enemy's action messages immediately after processing them to the api
                JsonNode msgs = newState.get("messages");
                if (msgs != null && msgs.isArray()) {
                    for (JsonNode msg : msgs) {
                        final String text = msg.asText();
                        Platform.runLater(() -> appendLine(text));
                    }
                }
            } catch (ApiException e) {
                Platform.runLater(() -> appendLine("Enemy action error: " + e.getMessage()));
                break;
            }

            //add a delay between enemy attacks
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        return state;
    }

    //is a unit belonging to the player party?
    private boolean isPlayerUnit(String unitName, JsonNode partyA) {
        if (partyA == null || !partyA.has("units")) return false;
        for (JsonNode u : partyA.get("units")) {
            if (u.has("name") && u.get("name").asText().equals(unitName)) return true;
        }
        return false;
    }

    //handle campaign actions
    private JsonNode parseCampaignAction(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String action = parts[0].toLowerCase();

        return switch (action) {
            case "next", "quit", "view-party", "resolve-battle" ->
                    api.campaignAction(sessionId, action);
            case "use-item" -> {
                //format: use-item <item> <unit>
                if (parts.length < 2) throw new ApiException("Usage: use-item <item> <unit>");
                String[] args = parts[1].split("\\s+", 2);
                if (args.length < 2) throw new ApiException("Usage: use-item <item> <unit>");
                yield api.campaignAction(sessionId, "use-item", args[0], args[1], null, null);
            }
            case "level-up" -> {
                //format: level-up <unit> <class> OR just level-up (shows menu)
                if (parts.length < 2) {
                    yield api.campaignAction(sessionId, "level-up", null, null, null, null);
                }
                String[] args = parts[1].split("\\s+", 2);
                if (args.length < 2) {
                    yield api.campaignAction(sessionId, "level-up", null, null, null, null);
                }
                yield api.campaignAction(sessionId, "level-up", null, args[0], args[1], null);
            }
            case "buy" -> {
                //format: buy <item>
                if (parts.length < 2) throw new ApiException("Usage: buy <item>");
                yield api.campaignAction(sessionId, "buy", parts[1].trim(), null, null, null);
            }
            case "recruit" ->
                    api.campaignAction(sessionId, "recruit");
            case "confirm-recruit" -> {
                if (parts.length < 2) throw new ApiException("Usage: confirm-recruit <index>");
                yield api.campaignAction(sessionId, "confirm-recruit", null, null, null, Integer.parseInt(parts[1].trim()));
            }
            case "leave" ->
                    api.campaignAction(sessionId, "leave");
            default ->
                    throw new ApiException("Unknown command: " + action + "\nValid: next | quit | use-item | level-up | view-party | buy | recruit | leave");
        };
    }

    //handle battle inputs. Similar to above.
    private JsonNode handleBattleInput(String line) {
        String[] parts = line.trim().split("\\s+", 3);
        String action = parts[0].toLowerCase();

        return switch (action) {
            case "attack" -> {
                if (parts.length < 2) throw new ApiException("Usage: attack <target>");
                yield api.battleAction(activeBattleId, "attack", parts[1], null);
            }
            case "defend" -> api.battleAction(activeBattleId, "defend", null, null);
            case "wait" -> api.battleAction(activeBattleId, "wait", null, null);
            case "cast" -> {
                // format: cast <ability> <target>
                // Ability name may be multi-word. I.e, cast Chain Lightning Goblin
                // Last token is the target if the ability requires one,
                // but we can't know that here, so we send everything after "cast" as the
                // raw string and parse it. last word = target, everything before = ability.
                // Rejoin parts[1] and parts[2] (if present) to get the full argument string!
                String rest = parts.length > 1 ? parts[1] : "";
                if (parts.length > 2) rest = rest + " " + parts[2];
                rest = rest.replaceAll("\"", "").trim(); //strip any quotes
                if (rest.isBlank()) throw new ApiException("Usage: cast <ability> [target]");
                String[] tokens = rest.split("\\s+");
                String ability;
                String target;
                if (tokens.length == 1) {
                    ability = tokens[0];
                    target = null;
                } else {
                    //last token is the target, everything before that is the ability name
                    target = tokens[tokens.length - 1];
                    StringBuilder ab = new StringBuilder();
                    for (int i = 0; i < tokens.length - 1; i++) {
                        if (i > 0) ab.append(" ");
                        ab.append(tokens[i]);
                    }
                    ability = ab.toString();
                }
                yield api.battleAction(activeBattleId, "cast", target, ability);
            }
            default -> throw new ApiException("Unknown battle command: " + action);
        };
    }

    /**
     * displays server state: prints all messages, updates room label,
     * and switches battle mode on/off based on phase.
     */
    private void displayState(JsonNode state) {
        //print all messages from this response
        JsonNode messages = state.get("messages");
        if (messages != null && messages.isArray()) {
            for (JsonNode msg : messages) appendLine(msg.asText());
        }

        //update room counter if available
        if (state.has("currentRoom") && state.has("totalRooms")) {
            currentRoom = state.get("currentRoom").asInt();
            int total = state.get("totalRooms").asInt();
            campaignLabel.setText(campaignName + " - Room " + currentRoom + " / " + total);
        }

        //track current party for saving on quit
        if (state.has("playerParty")) {
            try {
                currentParty = GameMapper.fromJson(state.get("playerParty").toString(), Party.class);
                partyName = currentParty.getName();
            } catch (Exception ignored) {}
        }

        //track battle phase - check multiple possible field names
        String phase = state.has("phase") ? state.get("phase").asText() : "";
        if ("BATTLE".equals(phase)) {
            String prevBattleId = activeBattleId;
            if (state.has("activeBattleSessionId") && !state.get("activeBattleSessionId").isNull()) {
                activeBattleId = state.get("activeBattleSessionId").asText();
            } else if (state.has("battleSessionId") && !state.get("battleSessionId").isNull()) {
                activeBattleId = state.get("battleSessionId").asText();
            }
            //extract battleId from message text as a fallback (might not be necessary anymore. Check discord March 29)
            if (activeBattleId == null && state.has("messages")) {
                for (JsonNode msg : state.get("messages")) {
                    String text = msg.asText();
                    if (text.contains("battleId:")) {
                        activeBattleId = text.substring(text.indexOf("battleId:") + 9).trim();
                        if (activeBattleId.contains(" "))
                            activeBattleId = activeBattleId.substring(0, activeBattleId.indexOf(" "));
                    }
                }
            }
            if (activeBattleId != null && !activeBattleId.equals(prevBattleId)) {
                appendLine("");
                appendLine("=== BATTLE MODE — Commands: attack <target> | defend | wait | cast \"<ability>\" <target> ===");
                appendLine("");
                //initial battle state (teams + first turn prompt) is retrieved on the campaign-action thread and stored in initialBattleState, then printed here in order.
            }
        } else {
            activeBattleId = null;
        }
    }

    /**
     * Prints the messages returned by the battle-service. It's already
     * Formatted so no need to do anything fancy here.
     */
    private void displayBattleState(JsonNode state) {
        JsonNode messages = state.get("messages");
        if (messages != null && messages.isArray()) {
            for (JsonNode msg : messages) {
                appendLine(msg.asText());
            }
        }
        //TODO: remove this line
        if (state.has("finished") && state.get("finished").asBoolean()) {
            appendLine("Type: resolve-battle");
        }
    }

    private void appendLine(String line) {console.appendText(line + "\n");}

    //For the input box. Prevents the user from interrupting AI input flow by spamming inputs.
    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        submitBtn.setDisable(!enabled);
    }

    public VBox getRoot() { return root; }
}
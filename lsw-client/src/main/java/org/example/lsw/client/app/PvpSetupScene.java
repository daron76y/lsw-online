package org.example.lsw.client.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.lsw.client.api.ApiException;
import org.example.lsw.core.Party;

import java.util.List;

/**
 * PvP setup flow - two steps shown in the same scene, swapping content:
 *   STEP 1: Player 1 enters Player 2's username, then picks their own PvP party.
 *   STEP 2: Player 2 picks their PvP party (Player 1 looks away).
 * Once both parties are chosen, SceneManager launches the PvP battle.
 */
public class PvpSetupScene {
    private final VBox root;
    private final StackPane contentArea; // swapped between step 1 and step 2

    public PvpSetupScene(SceneManager sm) {
        // -----------------------------------------------------------------------
        // |                              Top bar                                |
        // -----------------------------------------------------------------------
        Button backBtn = new Button("← Back");
        backBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #aaaaaa;
            -fx-cursor: hand;
            -fx-font-size: 13px;
        """);
        backBtn.setOnAction(e -> sm.showMainMenu());

        Label title = new Label("PvP Match Setup");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #e0e0e0;");

        HBox topBar = new HBox(backBtn, new Spacer(), title, new Spacer());
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 20, 8, 20));

        // -----------------------------------------------------------------------
        // |                          Content area                               |
        // -----------------------------------------------------------------------
        contentArea = new StackPane();
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Start on step 1
        showStep1(sm);

        root = new VBox(topBar, contentArea);
        root.setStyle("-fx-background-color: #1a1a1a;");
    }

    // -----------------------------------------------------------------------
    // |           Step 1 - pick opponent and choose party                   |
    // -----------------------------------------------------------------------

    private void showStep1(SceneManager sm) {
        String p1Username = sm.getSession().getUsername();
        List<Party> p1Parties = sm.getSession().getPvpParties();

        Label p1Label = new Label("Player 1: " + p1Username);
        p1Label.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        p1Label.setStyle("-fx-text-fill: #4a9eff;");

        // Player 2 username field
        Label p2Title = new Label("Enter Player 2's username:");
        p2Title.setStyle("-fx-text-fill: #cccccc;");

        TextField p2Field = new TextField();
        p2Field.setPromptText("Player 2 username");
        p2Field.setMaxWidth(300);

        Label partyTitle = new Label("Player 1 — choose your PvP party:");
        partyTitle.setStyle("-fx-text-fill: #cccccc;");

        ToggleGroup p1Toggle = new ToggleGroup();
        VBox p1List = buildPartyPicker(p1Parties, p1Toggle, "#4a9eff");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #cc3333; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        Button nextBtn = new Button("Next: Player 2 picks →");
        nextBtn.setPrefWidth(220);
        styleBtn(nextBtn, "#3a7bd5");
        nextBtn.setOnAction(e -> {
            // Validate Player 2 username
            String p2Name = p2Field.getText().trim();
            if (p2Name.isBlank()) { errorLabel.setText("Enter Player 2's username."); errorLabel.setVisible(true); return; }
            if (p2Name.equalsIgnoreCase(p1Username)) { errorLabel.setText("Player 2 must be a different user."); errorLabel.setVisible(true); return; }

            // Validate Player 1 party selection
            Toggle sel = p1Toggle.getSelectedToggle();
            if (sel == null) { errorLabel.setText("Select your PvP party."); errorLabel.setVisible(true); return; }
            Party p1Party = (Party) sel.getUserData();

            // Fetch player 2's profile to get their parties
            try {
                com.fasterxml.jackson.databind.JsonNode p2Profile = sm.getApi().getProfile(p2Name);
                org.example.lsw.client.api.ClientSession p2Session = org.example.lsw.client.api.ClientSession.fromJson(p2Profile);
                if (p2Session.getPvpParties().isEmpty()) { errorLabel.setText(p2Name + " has no PvP parties yet."); errorLabel.setVisible(true); return; }

                // Advance to step 2
                showStep2(sm, p1Party, p2Name, p2Session.getPvpParties());
            } catch (ApiException ex) { errorLabel.setText(ex.getMessage()); errorLabel.setVisible(true); }
        });

        contentArea.getChildren().setAll(buildCard(p1Label, p2Title, p2Field, partyTitle, p1List, errorLabel, nextBtn));
    }

    // -----------------------------------------------------------------------
    // |                   Step 2 - player 2 picks party                     |
    // -----------------------------------------------------------------------

    private void showStep2(SceneManager sm, Party p1Party, String p2Username, List<Party> p2Parties) {
        Label handoff = new Label("Hand the device to " + p2Username);
        handoff.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        handoff.setStyle("-fx-text-fill: #e0a030;");

        Label p2Label = new Label("Player 2: " + p2Username);
        p2Label.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        p2Label.setStyle("-fx-text-fill: #e05050;");

        Label partyTitle = new Label("Player 2 — choose your PvP party:");
        partyTitle.setStyle("-fx-text-fill: #cccccc;");

        ToggleGroup p2Toggle = new ToggleGroup();
        VBox p2List = buildPartyPicker(p2Parties, p2Toggle, "#e05050");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #cc3333; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        Button fightBtn = new Button("⚔  Start Battle!");
        fightBtn.setPrefWidth(220);
        styleBtn(fightBtn, "#7b2020");
        fightBtn.setOnAction(e -> {
            Toggle sel = p2Toggle.getSelectedToggle();
            if (sel == null) { errorLabel.setText("Select a PvP party."); errorLabel.setVisible(true); return; }
            Party p2Party = (Party) sel.getUserData();
            sm.startPvpMatch(p2Username, p1Party, p2Party);
        });

        contentArea.getChildren().setAll(buildCard(handoff, p2Label, partyTitle, p2List, errorLabel, fightBtn));
    }

    // -----------------------------------------------------------------------
    // |                              Helpers                                |
    // -----------------------------------------------------------------------

    private VBox buildPartyPicker(List<Party> parties, ToggleGroup tg, String accent) {
        VBox box = new VBox(6);
        for (Party p : parties) {
            ToggleButton btn = new ToggleButton(p.getName() + "  [" + p.getUnits().size() + " heroes]");
            btn.setToggleGroup(tg); btn.setUserData(p);
            btn.setPrefWidth(Double.MAX_VALUE);
            btn.setStyle(unselectedParty());
            btn.selectedProperty().addListener((obs, was, now) ->
                btn.setStyle(now ? selParty(accent) : unselectedParty()));
            box.getChildren().add(btn);
        }
        return box;
    }

    private VBox buildCard(javafx.scene.Node... nodes) {
        VBox card = new VBox(14, nodes);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(32));
        card.setMaxWidth(520);
        card.setStyle("""
            -fx-background-color: #2b2b2b;
            -fx-background-radius: 12;
            -fx-border-color: #444;
            -fx-border-radius: 12;
            -fx-border-width: 1;
        """);
        StackPane.setAlignment(card, Pos.CENTER);
        return card;
    }

    //stylers
    private static void styleBtn(Button b, String color) {
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 10 20 10 20;");
    }
    private static String unselectedParty() {return "-fx-background-color: #3a3a3a; -fx-text-fill: #cccccc; -fx-background-radius: 6; -fx-border-color: #555; -fx-border-radius: 6; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 8;";}
    private static String selParty(String a) {return "-fx-background-color: #1a2a4a; -fx-text-fill: white; -fx-background-radius: 6; -fx-border-color: " + a + "; -fx-border-radius: 6; -fx-border-width: 2; -fx-cursor: hand; -fx-padding: 8;";}

    //other
    public VBox getRoot() {return root;}
    private static class Spacer extends Region {Spacer() { HBox.setHgrow(this, Priority.ALWAYS); }}
}

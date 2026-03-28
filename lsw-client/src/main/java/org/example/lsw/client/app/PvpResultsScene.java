package org.example.lsw.client.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Results screen shown after a PvP battle ends.
 * Displays:
 *   - Winner announcement
 *   - Both players' updated W/L records
 *   - "Back to Menu" button (returns to Player 1's main menu)
 */
public class PvpResultsScene {
    private final VBox root;

    public PvpResultsScene(SceneManager sm, String winnerUsername, String p1Username, String p1PartyName, String p2Username, String p2PartyName) {
        // -----------------------------------------------------------------------
        // |                               Winner                                |
        // -----------------------------------------------------------------------
        Label trophy = new Label("🏆");
        trophy.setFont(Font.font(52));

        Label winnerLabel = new Label(winnerUsername + " wins!");
        winnerLabel.setFont(Font.font("Serif", FontWeight.BOLD, 28));
        winnerLabel.setStyle("-fx-text-fill: #f0c040;");

        // -----------------------------------------------------------------------
        // |                           Stats card                                |
        // -----------------------------------------------------------------------
        HBox statsRow = new HBox(20,
            buildCard(p1Username, p1PartyName, p1Username.equals(winnerUsername)),
            buildCard(p2Username, p2PartyName, p2Username.equals(winnerUsername))
        );
        statsRow.setAlignment(Pos.CENTER);

        // -----------------------------------------------------------------------
        // |                                Back                                 |
        // -----------------------------------------------------------------------
        Button backBtn = new Button("Back to Menu");
        backBtn.setPrefWidth(200);
        backBtn.setStyle("""
            -fx-background-color: #3a7bd5;
            -fx-text-fill: white;
            -fx-font-size: 14px;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-padding: 10 20 10 20;
        """);
        backBtn.setOnAction(e -> sm.showMainMenu());

        // -----------------------------------------------------------------------
        // |                                Card                                 |
        // -----------------------------------------------------------------------
        VBox card = new VBox(16, trophy, winnerLabel, new Separator(), statsRow, backBtn);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(640);
        card.setStyle("""
            -fx-background-color: #2b2b2b;
            -fx-background-radius: 12;
            -fx-border-color: #f0c040;
            -fx-border-radius: 12;
            -fx-border-width: 1;
        """);

        root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #494033;");
    }

    //TODO: further simplify buildCard
    private VBox buildCard(String username, String partyName, boolean won) {
        String accent = won ? "#f0c040" : "#888888";
        Label name   = new Label(username);
        name.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        name.setStyle("-fx-text-fill: #e0e0e0;");
        Label party  = new Label(partyName);
        party.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
        Label result = new Label(won ? "VICTORY" : "DEFEAT");
        result.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        result.setStyle("-fx-text-fill: " + accent + ";");

        VBox card = new VBox(8, name, party, new Separator(), result);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setPrefWidth(240);
        card.setStyle("-fx-background-color: #333333; -fx-background-radius: 8; -fx-border-color: " + accent + "; -fx-border-radius: 8; -fx-border-width: 1;");
        return card;
    }

    public VBox getRoot() { return root; }
}

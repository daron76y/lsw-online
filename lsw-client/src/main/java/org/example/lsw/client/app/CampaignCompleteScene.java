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
 * Shown when a PvE campaign is completed (room 30 reached).
 * The player chooses to either:
 *   - SAVE  the party into their PvP roster (max 5). If the roster is full,
 *           they are first asked which existing party to replace.
 *   - DISCARD the party and return to the main menu
 * After saving, the campaign save entry is deleted from the user profile
 * and the player is returned to the main menu.
 */
public class CampaignCompleteScene {
    private final VBox root;

    public CampaignCompleteScene(SceneManager sm, Party completedParty, int score) {
        // -----------------------------------------------------------------------
        // |                               Header                                |
        // -----------------------------------------------------------------------
        Label trophy = new Label("🏆");
        trophy.setFont(Font.font(48));

        Label title = new Label("Campaign Complete!");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 26));
        title.setStyle("-fx-text-fill: #f0c040;");

        Label subtitle  = new Label("\"" + completedParty.getName() + "\" has conquered all rooms!");
        subtitle.setFont(Font.font("Serif", 15));
        subtitle.setStyle("-fx-text-fill: #aaaaaa;");

        Label scoreLabel = new Label("Final Score: " + score);
        scoreLabel.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        scoreLabel.setStyle("-fx-text-fill: #f0c040;");

        // -----------------------------------------------------------------------
        // |                           Party summary                             |
        // -----------------------------------------------------------------------
        VBox unitList = new VBox(4);
        completedParty.getUnits().forEach(u -> {
            Label ul = new Label("• " + u.getName() + "  [" + u.getMainClass() + " Lv." + u.getLevel() + "]"
                    + "  ATK:" + u.getAttack() + "  DEF:" + u.getDefense());
            ul.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
            unitList.getChildren().add(ul);
        });

        // -----------------------------------------------------------------------
        // |                              Buttons                                |
        // -----------------------------------------------------------------------
        Button saveBtn    = new Button("Save Party for PvP"); saveBtn.setPrefWidth(200);
        styleBtn(saveBtn, "#3a7bd5");
        Button discardBtn = new Button("Discard & Return to Menu"); discardBtn.setPrefWidth(200);
        styleBtn(discardBtn, "#5a5a5a");
        Label statusLabel = new Label(); statusLabel.setStyle("-fx-text-fill: #cc3333; -fx-font-size: 12px;");
        HBox btnRow = new HBox(16, saveBtn, discardBtn); btnRow.setAlignment(Pos.CENTER);

        // -----------------------------------------------------------------------
        // |                           Replace party                             |
        // -----------------------------------------------------------------------
        Label replaceLabel = new Label("PvP roster is full. Choose a party to replace:");
        replaceLabel.setStyle("-fx-text-fill: #e0a030;"); replaceLabel.setVisible(false); replaceLabel.setManaged(false);
        VBox replaceList = new VBox(6); replaceList.setVisible(false); replaceList.setManaged(false);

        // -----------------------------------------------------------------------
        // |                              Handlers                               |
        // -----------------------------------------------------------------------
        discardBtn.setOnAction(e -> {
            discardBtn.setDisable(true);
            new Thread(() -> {
                try {
                    sm.getApi().deleteCampaignProgress(sm.getSession().getUsername(), completedParty.getName() + "'s Campaign");
                    sm.getApi().deleteParty(sm.getSession().getUsername(), completedParty.getName());
                } catch (ApiException ignored) {}
                javafx.application.Platform.runLater(sm::showMainMenu);
            }, "discard").start();
        });

        saveBtn.setOnAction(e -> {
            List<Party> pvpParties = sm.getSession().getPvpParties();
            if (pvpParties.size() < 5) {
                commitSave(sm, completedParty, -1, statusLabel);
            } else {
                saveBtn.setDisable(true);
                replaceLabel.setVisible(true); replaceLabel.setManaged(true);
                replaceList.getChildren().clear();
                for (int i = 0; i < pvpParties.size(); i++) {
                    final int slot = i;
                    Party existing = pvpParties.get(i);
                    Button rb = new Button(existing.getName() + "  [" + existing.getUnits().size() + " heroes]");
                    rb.setPrefWidth(Double.MAX_VALUE);
                    styleBtn(rb, "#6a2020");
                    rb.setOnAction(re -> commitSave(sm, completedParty, slot, statusLabel));
                    replaceList.getChildren().add(rb);
                }
                replaceList.setVisible(true); replaceList.setManaged(true);
            }
        });

        // -----------------------------------------------------------------------
        // |                                Card                                 |
        // -----------------------------------------------------------------------
        VBox card = new VBox(14, trophy, title, subtitle, scoreLabel,
                    new Separator(), unitList,
                    new Separator(), btnRow, statusLabel, replaceLabel, replaceList);
        card.setAlignment(Pos.CENTER); card.setPadding(new Insets(36)); card.setMaxWidth(580);
        card.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 12; -fx-border-color: #f0c040; -fx-border-radius: 12; -fx-border-width: 1;");

        root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #1a1a1a;");
    }

    /**
     * Performs the actual save and removes the party from campaign saves,
     * adds (or replaces) it in the PvP roster, saves, and returns to menu.
     *
     * @param slot  the roster index to replace, or -1 to append
     */
    private void commitSave(SceneManager sm, Party party, int slot, Label statusLabel) {
        new Thread(() -> {
            try {
                sm.getApi().deleteCampaignProgress(sm.getSession().getUsername(), party.getName() + "'s Campaign");
                sm.getApi().deleteParty(sm.getSession().getUsername(), party.getName());
                if (slot < 0) sm.getApi().savePvpParty(sm.getSession().getUsername(), party);
                else sm.getApi().replacePvpParty(sm.getSession().getUsername(), slot, party);
                javafx.application.Platform.runLater(sm::showMainMenu);
            } catch (ApiException e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setVisible(true);
                });
            }
        }, "commit-save").start();
    }

    //button styler helper
    private static void styleBtn(Button b, String color) {
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 10;");
    }

    public VBox getRoot() { return root; }
}
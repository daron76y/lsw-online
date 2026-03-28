package org.example.lsw.client.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.lsw.client.api.ClientSession;

import java.util.List;

/**
 * The main menu shown after login.
 * Layout:
 *   TOP    - "Welcome, username!" + Logout button
 *   LEFT   - Saved Campaigns list (each row has a "Resume" button) + "New Campaign" button at the bottom
 *   RIGHT  - PVP section
 */
public class MainMenuScene {
    private final VBox root;

    public MainMenuScene(SceneManager sm) {
        ClientSession session = sm.getSession();

        // -----------------------------------------------------------------------
        // |                              Top bar                                |
        // -----------------------------------------------------------------------
        Label welcome = new Label("Welcome back, " + session.getUsername() + "!");
        welcome.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        welcome.setStyle("-fx-text-fill: #e0e0e0;");

        Button logoutBtn = new Button("Log Out");
        logoutBtn.setStyle("-fx-background-color: #5a5a5a; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> sm.showLogin());

        HBox topBar = new HBox(welcome, new Spacer(), logoutBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 20, 10, 20));

        // -----------------------------------------------------------------------
        // |                    saved campaigns left panel                       |
        // -----------------------------------------------------------------------
        Label campaignsTitle = new Label("Your Campaigns");
        campaignsTitle.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        campaignsTitle.setStyle("-fx-text-fill: #cccccc;");

        VBox campaignList = new VBox(8);
        campaignList.setPadding(new Insets(8, 0, 8, 0));

        List<ClientSession.CampaignSave> saves = session.getCampaignSaves();
        if (saves.isEmpty()) {
            Label empty = new Label("No saved campaigns yet.");
            empty.setStyle("-fx-text-fill: #888888;");
            campaignList.getChildren().add(empty);
        } else {
            for (ClientSession.CampaignSave save : saves)
                campaignList.getChildren().add(buildCampaignRow(save, sm));
        }

        ScrollPane scroll = new ScrollPane(campaignList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button newBtn = new Button("+ New Campaign");
        newBtn.setPrefWidth(Double.MAX_VALUE);
        newBtn.setStyle("-fx-background-color: #3a7bd5; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 10;");
        newBtn.setOnAction(e -> sm.showNewCampaign());

        VBox left = new VBox(12, campaignsTitle, scroll, newBtn);
        left.setPadding(new Insets(16));
        left.setPrefWidth(260);
        left.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 10; -fx-border-color: #444; -fx-border-radius: 10; -fx-border-width: 1;");
        HBox.setHgrow(left, Priority.ALWAYS);

        // -----------------------------------------------------------------------
        // |                          Pvp right panel                            |
        // -----------------------------------------------------------------------
        Label pvpTitle = new Label("PvP Battle");
        pvpTitle.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        pvpTitle.setStyle("-fx-text-fill: #cccccc;");

        // W/L record
        Label wlLabel = new Label("Record:  " + session.getPvpWins() + "W  /  " + session.getPvpLosses() + "L");
        wlLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13px;");

        // PvP party count
        int pvpPartyCount = session.getPvpParties().size();
        Label pvpCountLabel = new Label("PvP parties: " + pvpPartyCount + " / 5");
        pvpCountLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13px;");

        boolean canStartPvp = pvpPartyCount > 0;
        Button pvpBtn = new Button(canStartPvp ? "Find Opponent" : "No PvP Parties Yet");
        pvpBtn.setPrefWidth(Double.MAX_VALUE);
        pvpBtn.setDisable(!canStartPvp);
        pvpBtn.setStyle(canStartPvp ? """
            -fx-background-color: #7b2020;
            -fx-text-fill: white;
            -fx-font-size: 13px;
            -fx-background-radius: 6;
            -fx-cursor: hand;
            -fx-padding: 10;
        """ : """
            -fx-background-color: #444444;
            -fx-text-fill: #888888;
            -fx-font-size: 13px;
            -fx-background-radius: 6;
            -fx-padding: 10;
        """);
        if (canStartPvp) pvpBtn.setOnAction(e -> sm.showPvpSetup());

        Label pvpHint = new Label(canStartPvp ? "" : "Complete a PvE campaign to unlock PvP.");
        pvpHint.setWrapText(true);
        pvpHint.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px; -fx-font-style: italic;");

        VBox rightPanel = new VBox(12, pvpTitle, wlLabel, pvpCountLabel, new Spacer(), pvpHint, pvpBtn);
        rightPanel.setPadding(new Insets(16));
        rightPanel.setPrefWidth(260);
        rightPanel.setStyle("""
            -fx-background-color: #2b2b2b;
            -fx-background-radius: 10;
            -fx-border-color: #444;
            -fx-border-radius: 10;
            -fx-border-width: 1;
        """);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        // -----------------------------------------------------------------------
        // |                           Main content row                          |
        // -----------------------------------------------------------------------
        HBox content = new HBox(16, left, rightPanel);
        content.setPadding(new Insets(0, 20, 20, 20));
        VBox.setVgrow(content, Priority.ALWAYS);

        // -----------------------------------------------------------------------
        // |                                root                                 |
        // -----------------------------------------------------------------------
        root = new VBox(topBar, content);
        root.setStyle("-fx-background-color: #1a1a1a;");
    }

    /** Build one row in the saved-campaign list. */
    private HBox buildCampaignRow(ClientSession.CampaignSave save, SceneManager sm) {
        Label name = new Label(save.campaignName());
        name.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 14px;");
        Label room = new Label("Room " + save.currentRoom() + " / 30");
        room.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
        VBox info = new VBox(2, name, room);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button resumeBtn = new Button("Resume");
        resumeBtn.setStyle("""
            -fx-background-color: #2e7d32;
            -fx-text-fill: white;
            -fx-background-radius: 5;
            -fx-cursor: hand;
        """);
        resumeBtn.setOnAction(e -> sm.resumeCampaign(save));

        HBox row = new HBox(12, info, resumeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("""
            -fx-background-color: #3a3a3a;
            -fx-background-radius: 6;
        """);

        return row;
    }

    public VBox getRoot() { return root; }

    /** Spacer utility - fills remaining horizontal/vertical space in an HBox/VBox. */
    private static class Spacer extends Region {
        Spacer() {
            HBox.setHgrow(this, Priority.ALWAYS);
            VBox.setVgrow(this, Priority.ALWAYS);
        }
    }
}

package org.example.lsw.client.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.lsw.core.HeroClass;
import org.example.lsw.core.UnitFactoryCSV;

import java.util.List;
import java.util.Map;

/**
 * New campaign setup screen.
 * The player is shown a randomly generated hero name (they can re-roll it),
 * and picks one of the four base classes (ORDER, CHAOS, WARRIOR, MAGE).
 * They also give their party a name before hitting "Start Campaign".
 * Layout:
 *   TOP    - "New Campaign" title + Back button
 *   CENTER - Party name field
 *            Hero name field  + "Random" name button
 *            Class picker: four class-card buttons
 *            Selected class description
 *   BOTTOM - "Start Campaign" button
 */
public class NewCampaignScene {
    private static final List<HeroClass> BASE_CLASSES = List.of(
            HeroClass.ORDER,
            HeroClass.CHAOS,
            HeroClass.WARRIOR,
            HeroClass.MAGE
    );

    //descriptions shown when the player selects a class. Descriptions taken directly from the project description doc.
    private static final java.util.Map<HeroClass, String> CLASS_DESCRIPTIONS = java.util.Map.of(
            HeroClass.ORDER, "The powers of the universe need to be balanced! Balance brings stability, and stability brings prosperity. Within balance you can channel your inner energy and protect those around you. Servants of order have higher intelligence, can heal and protect.",
            HeroClass.CHAOS, "The universe expands and cracks. Through the ripples untamed energy flows! Servants of chaos can harness this energy and launch devastating attacks!\n\n",
            HeroClass.WARRIOR, "Warriors are the blunt instruments of the world. They can enforce order or cause chaos. Some are mindless brutes, others are intelligent strategist. Who would you choose?\n\n",
            HeroClass.MAGE, "Those who wield wands should be feared! Who knows what goes in their mind or whom they serve? Will they aid you or hinder you? Powerful spells that can heal or damage, all at the flick of the wand!"
    );

    private final VBox root;

    public NewCampaignScene(SceneManager sm) {
        // -----------------------------------------------------------------------
        // |                                State                                |
        // -----------------------------------------------------------------------
        final String[] heroNameHolder = {generateHeroName()};
        final HeroClass[] selectedClass = {null};

        // -----------------------------------------------------------------------
        // |                               Top bar                                |
        // -----------------------------------------------------------------------
        Button backBtn = new Button("← Back");
        backBtn.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #aaaaaa;
            -fx-cursor: hand;
            -fx-font-size: 13px;
        """);
        backBtn.setOnAction(e -> sm.showMainMenu());

        Label title = new Label("New Campaign");
        title.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #e0e0e0;");

        HBox topBar = new HBox(backBtn, new Spacer(), title, new Spacer());
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(16, 20, 8, 20));

        // -----------------------------------------------------------------------
        // |                           Party Name                                |
        // -----------------------------------------------------------------------
        Label partyNameLabel = new Label("Party Name");
        partyNameLabel.setStyle("-fx-text-fill: #cccccc;");
        TextField partyNameField = new TextField();
        partyNameField.setMaxWidth(400);
        applyFieldStyle(partyNameField);

        // -----------------------------------------------------------------------
        // |                            Hero name                                |
        // -----------------------------------------------------------------------
        Label heroNameLabel = new Label("Starting Hero");
        heroNameLabel.setStyle("-fx-text-fill: #cccccc;");

        TextField heroNameField = new TextField(heroNameHolder[0]);
        heroNameField.setMaxWidth(300);
        applyFieldStyle(heroNameField);

        Button rerollBtn = new Button("🎲 Random");
        rerollBtn.setStyle("""
            -fx-background-color: #555555;
            -fx-text-fill: #dddddd;
            -fx-background-radius: 5;
            -fx-cursor: hand;
        """);
        rerollBtn.setOnAction(e -> heroNameField.setText(generateHeroName()));

        HBox heroNameRow = new HBox(10, heroNameField, rerollBtn);
        heroNameRow.setAlignment(Pos.CENTER_LEFT);

        // -----------------------------------------------------------------------
        // |                            Class picker                             |
        // -----------------------------------------------------------------------
        Label classLabel = new Label("Choose Starting Class");
        classLabel.setStyle("-fx-text-fill: #cccccc;");

        Label classDesc = new Label("Select a class to see its description.");
        classDesc.setWrapText(true);
        classDesc.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
        classDesc.setMaxWidth(480);

        //Class card buttons - one per base class
        ToggleGroup classToggle = new ToggleGroup();
        HBox classRow = new HBox(12);
        classRow.setAlignment(Pos.CENTER_LEFT);

        for (HeroClass hc : BASE_CLASSES) {
            ToggleButton btn = new ToggleButton(hc.name());
            btn.setToggleGroup(classToggle);
            btn.setPrefWidth(110); btn.setPrefHeight(60);
            btn.setStyle(unselectedCardStyle());
            btn.selectedProperty().addListener((obs, was, now) -> {
                if (now) {
                    selectedClass[0] = hc;
                    btn.setStyle(selectedCardStyle());
                    classDesc.setText(CLASS_DESCRIPTIONS.get(hc));
                } else {
                    btn.setStyle(unselectedCardStyle());
                }
            });
            classRow.getChildren().add(btn);
        }

        // -----------------------------------------------------------------------
        // |                            Error label                              |
        // -----------------------------------------------------------------------
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #cc3333; -fx-font-size: 12px;");
        errorLabel.setVisible(false);

        // -----------------------------------------------------------------------
        // |                                Start                                |
        // -----------------------------------------------------------------------
        Button startBtn = new Button("Start Campaign");
        startBtn.setPrefWidth(220);
        startBtn.setStyle("-fx-background-color: #3a7bd5; -fx-text-fill: white; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 10 20 10 20;");
        startBtn.setOnAction(e -> {
            String partyName = partyNameField.getText().trim();
            String heroName  = heroNameField.getText().trim();

            //validation with proper error messages
            if (partyName.isBlank()) {
                errorLabel.setText("Please enter a party name.");
                errorLabel.setVisible(true);
                return;
            }
            if (heroName.isBlank()) {
                errorLabel.setText("Please enter a hero name.");
                errorLabel.setVisible(true);
                return;
            }
            if (selectedClass[0] == null) {
                errorLabel.setText("Please choose a starting class.");
                errorLabel.setVisible(true);
                return;
            }

            sm.startNewCampaign(heroName, selectedClass[0], partyName);
        });

        // -----------------------------------------------------------------------
        // |                               Layout                                |
        // -----------------------------------------------------------------------
        VBox card = new VBox(18,
            partyNameLabel, partyNameField,
            heroNameLabel, heroNameRow,
            classLabel, classRow,
            classDesc, errorLabel, startBtn);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(32));
        card.setMaxWidth(560);
        card.setStyle("""
            -fx-background-color: #2b2b2b;
            -fx-background-radius: 12;
            -fx-border-color: #444;
            -fx-border-radius: 12;
            -fx-border-width: 1;
        """);

        // -----------------------------------------------------------------------
        // |                                Root                                 |
        // -----------------------------------------------------------------------
        VBox center = new VBox(card);
        center.setAlignment(Pos.CENTER);
        VBox.setVgrow(center, Priority.ALWAYS);

        root = new VBox(topBar, center);
        root.setStyle("-fx-background-color: #494033;");
    }

    // -----------------------------------------------------------------------
    // |                              Helpers                                |
    // -----------------------------------------------------------------------
    private String generateHeroName() {
        // Reuse the CSV factory's names via a temporary factory instance
        try {return new UnitFactoryCSV().generateHeroRecruits(1).getFirst().getName();}
        catch (Exception e) {return "GNS";}
    }

    private static void applyFieldStyle(TextField field) {
        field.setStyle("""
            -fx-background-color: #3a3a3a;
            -fx-text-fill: #e0e0e0;
            -fx-prompt-text-fill: #666666;
            -fx-border-color: #555555;
            -fx-border-radius: 5;
            -fx-background-radius: 5;
            -fx-padding: 6 10 6 10;
        """);
    }

    private static String unselectedCardStyle() {
        return """
            -fx-background-color: #3a3a3a;
            -fx-text-fill: #cccccc;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-border-color: #555555;
            -fx-border-radius: 8;
            -fx-border-width: 1;
            -fx-cursor: hand;
        """;
    }

    private static String selectedCardStyle() {
        return """
            -fx-background-color: #1a4a8a;
            -fx-text-fill: white;
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-background-radius: 8;
            -fx-border-color: #3a7bd5;
            -fx-border-radius: 8;
            -fx-border-width: 2;
            -fx-cursor: hand;
        """;
    }

    public VBox getRoot() { return root; }

    private static class Spacer extends Region {
        Spacer() {HBox.setHgrow(this, Priority.ALWAYS);}
    }
}

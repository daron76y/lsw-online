package org.example.lsw.client.app;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.lsw.client.api.ApiClient;
import org.example.lsw.client.api.ApiException;
import org.example.lsw.client.api.ClientSession;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.HeroClass;
import org.example.lsw.core.Items;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

import java.util.Map;

/**
 * Central controller for the GUI. It owns the Stage, holds the ApiClient and current session,
 * and drives all the scene transitions.
 * Interacts with the game logic via HTTP calls.
 */
public class SceneManager {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 650;

    private final Stage stage; //the actual JavaFX window itself
    private final ApiClient api = new ApiClient(); //the api client shared across the whole app
    private ClientSession session; //current logged-in players session

    public SceneManager(Stage stage) {
        this.stage = stage;
        stage.setTitle("Legends of Sword and Wand");
        stage.setOnCloseRequest(e -> System.exit(0));
    }

    //GETTERS
    public ApiClient getApi() { return api; }
    public ClientSession getSession() { return session; }

    // ════════════════════════════ Account Authentication ═════════════════════════════

    /**
     * Attempts login. Returns null on success, error message on failure.
     */
    public String login(String username, String password) {
        if (username.isBlank()) return "Username cannot be empty.";
        if (password.isBlank()) return "Password cannot be empty.";
        try {
            JsonNode profile = api.login(username, password);
            session = ClientSession.fromJson(profile);
            return null;
        } catch (ApiException e) { return e.getMessage(); }
    }

    /**
     * Attempts account creation. Returns null on success, error message on failure.
     */
    public String createAccount(String username, String password) {
        if (username.isBlank()) return "Username cannot be empty.";
        if (password.length() < 4) return "Password must be at least 4 characters.";
        try {
            JsonNode profile = api.register(username, password);
            session = ClientSession.fromJson(profile);
            return null;
        } catch (ApiException e) { return e.getMessage(); }
    }

    /**
     * Refreshes the session from server (calls after any change).
     */
    public void refreshSession() {
        try {
            session = ClientSession.fromJson(api.getProfile(session.getUsername()));
        } catch (ApiException ignored) {}
    }

    // ═══════════════════════════════ Scene Transitions ═══════════════════════════════

    /**
     * Login scene
     */
    public void showLogin() {
        LoginScene scene = new LoginScene(this);
        stage.setScene(new Scene(scene.getRoot(), WIDTH, HEIGHT));
        stage.show();
    }

    /**
     * Main Menu Scene
     */
    public void showMainMenu() {
        refreshSession();
        MainMenuScene scene = new MainMenuScene(this);
        stage.setScene(new Scene(scene.getRoot(), WIDTH, HEIGHT));
    }

    /**
     * Saves current campaign progress to user-service.
     * Called by GameScene whenever the player quits mid-campaign.
     */
    public void saveCampaignProgress(String campaignName, String partyName, int currentRoom, Party currentParty) {
        try {
            //update the party saved on the user profile with current state
            api.saveParty(session.getUsername(), currentParty);
            //update campaign progress room number
            api.saveCampaignProgress(session.getUsername(), campaignName, partyName, currentRoom);
        } catch (ApiException e) {
            System.err.println("Failed to save campaign progress: " + e.getMessage());
        }
    }

    /**
     * New campaign creation scene
     */
    public void showNewCampaign() {
        NewCampaignScene scene = new NewCampaignScene(this);
        stage.setScene(new Scene(scene.getRoot(), WIDTH, HEIGHT));
    }

    /**
     * Setup pvp match scene
     */
    public void showPvpSetup() {
        PvpSetupScene scene = new PvpSetupScene(this);
        stage.setScene(new Scene(scene.getRoot(), WIDTH, HEIGHT));
    }

    // ═══════════════════════════════ Campaign Methods ════════════════════════════════

    /**
     * Creates a new party and campaign session and
     * saves them to the user profile, then opens the game screen.
     */
    public void startNewCampaign(String heroName, HeroClass heroClass, String partyName) {
        Party party = new Party(partyName);
        party.setGold(200);
        Unit startingHero = new Unit(heroName, heroClass);
        party.addUnit(startingHero);

        try {
            //save party to user profile
            api.saveParty(session.getUsername(), party);

            //create campaign session on the campaign-service
            JsonNode response = api.startCampaign(session.getUsername(), party);
            String sessionId = response.get("sessionId").asText();

            //save campaign progress entry on user-service
            api.saveCampaignProgress(session.getUsername(), partyName + "'s Campaign", partyName, 0);

            //set this campaign to the active session, for this logged-in players client session. Then show game scene.
            session.setActiveCampaignSessionId(sessionId);
            showGameScene(sessionId, partyName + "'s Campaign");
        } catch (ApiException e) {
            showError("Failed to start campaign: " + e.getMessage());
        }
    }

    /**
     * Resumes an existing campaign from the saved progress
     */
    public void resumeCampaign(ClientSession.CampaignSave save) {
        try {
            Party party = session.getSavedParties().stream()
                    .filter(p -> p.getName().equals(save.partyName()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Saved party not found: " + save.partyName()));

            //resume from the saved room number
            JsonNode response = api.startCampaign(session.getUsername(), party, save.currentRoom());
            String sessionId = response.get("sessionId").asText();

            //set this campaign to the active session, for this logged-in players client session. Then show game scene.
            session.setActiveCampaignSessionId(sessionId);
            showGameScene(sessionId, save.campaignName());
        } catch (ApiException e) {
            showError("Failed to resume campaign: " + e.getMessage());
        }
    }

    /**
     * Game Scene
     */
    private void showGameScene(String sessionId, String campaignName) {
        GameScene scene = new GameScene(this, sessionId, campaignName);
        stage.setScene(new Scene(scene.getRoot(), WIDTH, HEIGHT));
        scene.startGame();
    }

    /**
     * Campaign Complete Scene
     * Called by GameScene when the campaign is complete.
     * Calculates score and shows the completion screen.
     * TODO: remove duplicate score calculation
     */
    public void showCampaignComplete(String sessionId, Party completedParty) {
        int score = completedParty.getUnits().stream().mapToInt(Unit::getLevel).sum() * 100;
        score += completedParty.getGold() * 10;
        for (Map.Entry<Items, Integer> entry : completedParty.getInventory().entrySet())
            score += (entry.getKey().getCost() / 2) * 10 * entry.getValue();

        //update this score to the players account
        try {api.addScore(session.getUsername(), score);}
        catch (ApiException ignored) {}

        //show the campaignCompleteScene
        CampaignCompleteScene scene = new CampaignCompleteScene(this, completedParty, score);
        stage.setScene(new Scene(scene.getRoot(), WIDTH, HEIGHT));
    }

    // ══════════════════════════════════ Pvp Methods ══════════════════════════════════

    /**
     * Starts a PVP match. Called from PvpSetupScene once both parties are chosen
     */
    public void startPvpMatch(String p2Username, Party p1Party, Party p2Party) {
        try {
            //create a new pvp match and set this active session to associate with it
            JsonNode response = api.startPvpMatch(session.getUsername(), p1Party, p2Username, p2Party);
            String matchId = response.get("matchId").asText();
            String battleId = response.get("battleSessionId").asText();
            session.setActivePvpMatchId(matchId);
            session.setActiveBattleSessionId(battleId);

            //create a new pvp gameScene
            PvpGameScene scene = new PvpGameScene(
                    this,
                    matchId,
                    battleId,
                    session.getUsername(),
                    p1Party.getName(),
                    p2Username,
                    p2Party.getName()
            );
            stage.setScene(new Scene(scene.getRoot(), WIDTH, HEIGHT));
            scene.startMatch();
        } catch (ApiException e) {
            showError("Failed to start pvp match: " + e.getMessage());
        }
    }

    /**
     * Error utility to print a message and redirect back to the main menu
     */
    private void showError(String message) {
        System.err.println("SceneManager error: " + message);
        showMainMenu();
    }
}
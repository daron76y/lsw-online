package org.example.lsw.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the in-memory session state cache for the currently logged-in player.
 * Populated from the UserProfileDto returned by login/register,
 * and refreshed by re-calling getProfile() after any profile change.
 */
public class ClientSession {
    private String username;
    private int score;
    private int pvpWins;
    private int pvpLosses;
    private List<Party> savedParties = new ArrayList<>();
    private List<Party> pvpParties = new ArrayList<>();
    private List<CampaignSave> campaignSaves = new ArrayList<>();

    /** populated when a campaign session is active. */
    private String activeCampaignSessionId;

    /** populated when a battle is active (either pve or pvp). */
    private String activeBattleSessionId;

    /** Populated when a pvp match is active. */
    private String activePvpMatchId;

    public record CampaignSave(String campaignName, String partyName, int currentRoom) {}

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                  deserialize from json                   │
    //      └──────────────────────────────────────────────────────────┘
    public static ClientSession fromJson(JsonNode node) {
        ClientSession s = new ClientSession();
        s.username = node.get("username").asText();
        s.score = node.has("score") ? node.get("score").asInt() : 0;
        s.pvpWins = node.has("pvpWins") ? node.get("pvpWins").asInt() : 0;
        s.pvpLosses = node.has("pvpLosses") ? node.get("pvpLosses").asInt() : 0;
        s.savedParties = parseParties(node.get("savedParties"));
        s.pvpParties = parseParties(node.get("pvpParties"));
        s.campaignSaves = new ArrayList<>();

        //attempt to get all data from the campaign saves
        JsonNode saves = node.get("campaignSaves");
        if (saves != null && saves.isArray()) {
            for (JsonNode c : saves) {
                s.campaignSaves.add(new CampaignSave(
                        c.get("campaignName").asText(),
                        c.get("partyName").asText(),
                        c.get("currentRoom").asInt()
                ));
            }
        }
        return s;
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │            utility for deserializing parties             │
    //      └──────────────────────────────────────────────────────────┘
    private static List<Party> parseParties(JsonNode node) {
        List<Party> list = new ArrayList<>();
        if (node == null || !node.isArray()) return list;
        for (JsonNode p : node) {
            try {list.add(GameMapper.fromJson(p.toString(), Party.class));}
            catch (Exception ignored) {}
        }
        return list;
    }

    // ═════════════════════════ Accessor Methods For Jackson ══════════════════════════
    public String getUsername()                  { return username; }
    public int getScore()                        { return score; }
    public int getPvpWins()                      { return pvpWins; }
    public int getPvpLosses()                    { return pvpLosses; }
    public List<Party> getSavedParties()         { return savedParties; }
    public List<Party> getPvpParties()           { return pvpParties; }
    public List<CampaignSave> getCampaignSaves() { return campaignSaves; }
    public String getActiveCampaignSessionId()   { return activeCampaignSessionId; }
    public String getActiveBattleSessionId()     { return activeBattleSessionId; }
    public String getActivePvpMatchId()          { return activePvpMatchId; }
    public void setActiveCampaignSessionId(String id) { this.activeCampaignSessionId = id; }
    public void setActiveBattleSessionId(String id)   { this.activeBattleSessionId = id; }
    public void setActivePvpMatchId(String id)        { this.activePvpMatchId = id; }
}

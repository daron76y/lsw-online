package org.example.lsw.campaign;

import org.example.lsw.core.CampaignState;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

import java.util.List;

/**
 * The response data holder object returned after every campaign action and on GET /api/campaign/{id}.
 * Contains everything the client needs to show the overworld and inn screen to the console.
 * Only contains essential client-related data, which is simpler than returning an entire campaignState object.
 */
public class CampaignStateResponse {
    private String sessionId;
    private String username;
    private int currentRoom;
    private int totalRooms;
    private CampaignState.Phase phase;
    private String activeBattleSessionId;  //non-null when phase == BATTLE
    private Party playerParty;
    private boolean finished;
    private List<String> messages;
    private List<Unit> availableRecruits;  //non-null during recruit action

    public CampaignStateResponse() {}

    public static CampaignStateResponse from(CampaignState state) {
        CampaignStateResponse r = new CampaignStateResponse();
        r.sessionId             = state.getSessionId();
        r.username              = state.getUsername();
        r.currentRoom           = state.getCurrentRoom();
        r.totalRooms            = state.getTotalRooms();
        r.phase                 = state.getPhase();
        r.activeBattleSessionId = state.getActiveBattleSessionId();
        r.playerParty           = state.getPlayerParty();
        r.finished              = state.isFinished();
        r.messages              = state.getMessages();
        return r;
    }

    public String getSessionId()                                       { return sessionId; }
    public void setSessionId(String sessionId)                         { this.sessionId = sessionId; }
    public String getUsername()                                        { return username; }
    public void setUsername(String username)                           { this.username = username; }
    public int getCurrentRoom()                                        { return currentRoom; }
    public void setCurrentRoom(int currentRoom)                        { this.currentRoom = currentRoom; }
    public int getTotalRooms()                                         { return totalRooms; }
    public void setTotalRooms(int totalRooms)                          { this.totalRooms = totalRooms; }
    public CampaignState.Phase getPhase()                              { return phase; }
    public void setPhase(CampaignState.Phase phase)                    { this.phase = phase; }
    public String getActiveBattleSessionId()                           { return activeBattleSessionId; }
    public void setActiveBattleSessionId(String activeBattleSessionId) { this.activeBattleSessionId = activeBattleSessionId; }
    public Party getPlayerParty()                                      { return playerParty; }
    public void setPlayerParty(Party playerParty)                      { this.playerParty = playerParty; }
    public boolean isFinished()                                        { return finished; }
    public void setFinished(boolean finished)                          { this.finished = finished; }
    public List<String> getMessages()                                  { return messages; }
    public void setMessages(List<String> messages)                     { this.messages = messages; }
    public List<Unit> getAvailableRecruits()                           { return availableRecruits; }
    public void setAvailableRecruits(List<Unit> availableRecruits)     { this.availableRecruits = availableRecruits; }
}

package org.example.lsw.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of a pve campaign session stored in the database between http requests
 * Each HTTP action loads this state, changes it, saves it back, and finally returns output messages.
 */
@JsonAutoDetect(
        fieldVisibility    = JsonAutoDetect.Visibility.ANY,
        getterVisibility   = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class CampaignState {
    public enum RoomType {BATTLE, INN}
    public enum Phase {OVERWORLD, BATTLE, INN, FINISHED}

    private String sessionId;
    private String username;
    private Party playerParty;
    private int currentRoom = 0;
    private int totalRooms = 30;
    private int lastInnCheckpoint = 0;
    private RoomType[] roomTypes;
    private Phase phase = Phase.OVERWORLD;
    private String activeBattleSessionId;
    private List<String> messages = new ArrayList<>();
    private boolean finished = false;
    private List<Unit> availableRecruits = new ArrayList<>();

    private CampaignState() {}

    public CampaignState(String sessionId, String username, Party playerParty) {
        this.sessionId = sessionId;
        this.username = username;
        this.playerParty = playerParty;
        this.roomTypes = new RoomType[totalRooms + 1];
    }

    //getters and setters
    public String getSessionId()                    { return sessionId; }
    public String getUsername()                     { return username; }
    public Party getPlayerParty()                   { return playerParty; }
    public int getCurrentRoom()                     { return currentRoom; }
    public int getTotalRooms()                      { return totalRooms; }
    public int getLastInnCheckpoint()               { return lastInnCheckpoint; }
    public RoomType[] getRoomTypes()                { return roomTypes; }
    public Phase getPhase()                         { return phase; }
    public String getActiveBattleSessionId()        { return activeBattleSessionId; }
    public List<String> getMessages()               { return messages; }
    public boolean isFinished()                     { return finished; }

    public void setCurrentRoom(int r)               { this.currentRoom = r; }
    public void setLastInnCheckpoint(int c)         { this.lastInnCheckpoint = c; }
    public void setRoomType(int room, RoomType t)   { this.roomTypes[room] = t; }
    public void setPhase(Phase phase)               { this.phase = phase; }
    public void setActiveBattleSessionId(String id) { this.activeBattleSessionId = id; }
    public void setFinished(boolean finished)       { this.finished = finished; }
    public void setMessages(List<String> messages)  { this.messages = messages; }
    public List<Unit> getAvailableRecruits()        { return availableRecruits; }
    public void setAvailableRecruits(List<Unit> r)  { this.availableRecruits = r; }

    public void addMessage(String msg)              { this.messages.add(msg); }
    public void clearMessages()                     { this.messages.clear(); }
}
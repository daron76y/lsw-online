package org.example.lsw.user;

/**
 * data carrier for a saved campaign, holding its progress.
 * Stored as json inside the users.campaign_saves column in the database
 */
public class CampaignProgressDto {
    private String campaignName;
    private String partyName;
    private int currentRoom;

    public CampaignProgressDto() {}

    public CampaignProgressDto(String campaignName, String partyName, int currentRoom) {
        this.campaignName = campaignName;
        this.partyName = partyName;
        this.currentRoom = currentRoom;
    }

    public String getCampaignName() { return campaignName; }
    public void setCampaignName(String n) { this.campaignName = n; }
    public String getPartyName() { return partyName; }
    public void setPartyName(String n) { this.partyName = n; }
    public int getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(int r) { this.currentRoom = r; }
}

package org.example.lsw.campaign;

import org.example.lsw.core.Party;

/**
 * Request body for POSTing to /api/campaign/start
 */
public class StartCampaignRequest {
    private String username;
    private Party party;
    private int startRoom = 0; //default to 0 for starting a new campaign. Setting it to something else makes it resume a campaign.

    public StartCampaignRequest() {}
    public String getUsername()           { return username; }
    public void setUsername(String u)     { this.username = u; }
    public Party getParty()               { return party; }
    public void setParty(Party p)         { this.party = p; }
    public int getStartRoom()             { return startRoom; }
    public void setStartRoom(int r)       { this.startRoom = r; }
}
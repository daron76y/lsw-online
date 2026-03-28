package org.example.lsw.user;

import org.example.lsw.core.Party;
import java.util.List;

/**
 * the full user profile object returned by GET /api/users/{username}/profile.
 * Contains everything the client needs for output purposes
 */
public class UserProfileDto {
    private String username;
    private int score;
    private int pvpWins;
    private int pvpLosses;
    private List<Party> savedParties;
    private List<Party> pvpParties;
    private List<CampaignProgressDto> campaignSaves;

    public UserProfileDto() {}

    public UserProfileDto(UserEntity e) {
        this.username      = e.getUsername();
        this.score         = e.getScore();
        this.pvpWins       = e.getPvpWins();
        this.pvpLosses     = e.getPvpLosses();
        this.savedParties  = e.getSavedParties();
        this.pvpParties    = e.getPvpParties();
        this.campaignSaves = e.getCampaignSaves();
    }

    public String getUsername()                               { return username; }
    public void setUsername(String username)                  { this.username = username; }
    public int getScore()                                     { return score; }
    public void setScore(int score)                           { this.score = score; }
    public int getPvpWins()                                   { return pvpWins; }
    public void setPvpWins(int pvpWins)                       { this.pvpWins = pvpWins; }
    public int getPvpLosses()                                 { return pvpLosses; }
    public void setPvpLosses(int pvpLosses)                   { this.pvpLosses = pvpLosses; }
    public List<Party> getSavedParties()                      { return savedParties; }
    public void setSavedParties(List<Party> p)                { this.savedParties = p; }
    public List<Party> getPvpParties()                        { return pvpParties; }
    public void setPvpParties(List<Party> p)                  { this.pvpParties = p; }
    public List<CampaignProgressDto> getCampaignSaves()       { return campaignSaves; }
    public void setCampaignSaves(List<CampaignProgressDto> s) { this.campaignSaves = s; }
}

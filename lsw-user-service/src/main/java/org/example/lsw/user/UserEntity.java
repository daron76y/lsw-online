package org.example.lsw.user;

import jakarta.persistence.*;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Java entity that maps to the "users" table in the database
 * savedParties, pvpParties, and campaignSaves are stored as json text columns
 */
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private int score = 0;

    @Column(name = "pvp_wins", nullable = false)
    private int pvpWins = 0;

    @Column(name = "pvp_losses", nullable = false)
    private int pvpLosses = 0;

    //JSON-serialized lists of Party objects
    @Column(name = "saved_parties", columnDefinition = "TEXT")
    private String savedPartiesJson = "[]";

    @Column(name = "pvp_parties", columnDefinition = "TEXT")
    private String pvpPartiesJson = "[]";

    //JSON-serialized list of CampaignProgressDto's
    @Column(name = "campaign_saves", columnDefinition = "TEXT")
    private String campaignSavesJson = "[]";

    protected UserEntity() {}

    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }

    //accessor methods
    public String getUsername()             { return username; }
    public String getPassword()             { return password; }
    public int getScore()                   { return score; }
    public void setScore(int score)         { this.score = score; }
    public int getPvpWins()                 { return pvpWins; }
    public void setPvpWins(int pvpWins)     { this.pvpWins = pvpWins; }
    public int getPvpLosses()               { return pvpLosses; }
    public void setPvpLosses(int pvpLosses) { this.pvpLosses = pvpLosses; }

    // ══════════════════════════════ Json Column Helpers ══════════════════════════════
    public List<Party> getSavedParties() {
        return deserializeParties(savedPartiesJson);
    }
    public void setSavedParties(List<Party> parties) {
        this.savedPartiesJson = GameMapper.toJson(parties);
    }

    public List<Party> getPvpParties() {
        return deserializeParties(pvpPartiesJson);
    }
    public void setPvpParties(List<Party> parties) {
        this.pvpPartiesJson = GameMapper.toJson(parties);
    }

    public List<CampaignProgressDto> getCampaignSaves() {
        try {
            return GameMapper.get().readValue(
                campaignSavesJson,
                new TypeReference<List<CampaignProgressDto>>() {}
            );
        } catch (Exception e) { return new ArrayList<>(); }
    }
    public void setCampaignSaves(List<CampaignProgressDto> saves) {
        this.campaignSavesJson = GameMapper.toJson(saves);
    }

    private List<Party> deserializeParties(String json) {
        try {
            return GameMapper.get().readValue(
                json,
                new TypeReference<List<Party>>() {}
            );
        } catch (Exception e) { return new ArrayList<>(); }
    }
}

package org.example.lsw.campaign;

import jakarta.persistence.*;
import org.example.lsw.core.CampaignState;
import org.example.lsw.core.GameMapper;

/**
 * Java entity for the campaign_sessions table in the database.
 * The full CampaignState is stored as a single Json MEDIUMTEXT column,
 * Pretty much the same as BattleSessionEntity.
 */
@Entity
@Table(name = "campaign_sessions")
public class CampaignSessionEntity {
    //campaign session UUID
    @Id
    @Column(length = 36)
    private String id;

    //which player owns this campaign
    @Column(nullable = false, length = 50)
    private String username;

    //serialized campaignState
    @Column(name = "state_json", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String stateJson;

    //quick flag for if the campaign is finished or not
    @Column(nullable = false)
    private boolean finished = false;

    protected CampaignSessionEntity() {}

    public CampaignSessionEntity(CampaignState state) {
        this.id       = state.getSessionId();
        this.username = state.getUsername();
        this.stateJson = GameMapper.toJson(state);
        this.finished  = state.isFinished();
    }

    public String getId()       { return id; }
    public String getUsername() { return username; }
    public boolean isFinished() { return finished; }

    public CampaignState getState() {
        return GameMapper.fromJson(stateJson, CampaignState.class);
    }

    public void updateState(CampaignState state) {
        this.stateJson = GameMapper.toJson(state);
        this.finished  = state.isFinished();
    }
}

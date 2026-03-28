package org.example.lsw.battle;

import jakarta.persistence.*;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.battle.BattleState;

/**
 * Java object that maps to the battle_sessions table in the database.
 * The entire BattleState is serialized as a JSON TEXT column.
 * This is the cleanest approach we got come up with since BattleState
 * already has all the Jackson annotations needed, and GameMapper handles
 * the (de)serialization
 */
@Entity
@Table(name = "battle_sessions")
public class BattleSessionEntity {
    @Id
    @Column(length = 36)  //UUID for this battle
    private String id;

    /**
     * Who owns this battle session?
     * For pve: the campaign session id that started this battle.
     * For pvp: the pvp match id.
     */
    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "owner_type", nullable = false, length = 10)
    private String ownerType; // "PVE" or "PVP"

    /**
     * Fully serialized BattleState - both parties, turn queue, and messages, etc.
     * Stored as TEXT so it survives between HTTP requests with nothing in-memory
     */
    @Column(name = "state_json", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String stateJson;

    /**
     * Quick flag for checking if a battle is finished or not, without having to
     * fully deserialize the state
     */
    @Column(nullable = false)
    private boolean finished = false;

    // CONSTRUCTORS
    protected BattleSessionEntity() {}

    public BattleSessionEntity(String id, String ownerId, String ownerType, BattleState state) {
        this.id        = id;
        this.ownerId   = ownerId;
        this.ownerType = ownerType;
        this.stateJson = GameMapper.toJson(state);
        this.finished  = state.isFinished();
    }

    // ACCESSOR METHODS
    public String getId()        { return id; }
    public String getOwnerId()   { return ownerId; }
    public String getOwnerType() { return ownerType; }
    public boolean isFinished()  { return finished; }

    /**
     * Deserialize the state from the JSON as an actual BattleState object
     */
    public BattleState getState() {
        return GameMapper.fromJson(stateJson, BattleState.class);
    }

    /**
     * Re-serialize an updated  BattleState back into JSON and update its finished flag
     */
    public void updateState(BattleState state) {
        this.stateJson = GameMapper.toJson(state);
        this.finished  = state.isFinished();
    }
}

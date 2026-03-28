package org.example.lsw.pvp;

import jakarta.persistence.*;

/**
 * Tracks the lifecycle of a pvp match.
 * The actual battle itself is delegated entirely to battle-service.
 * This entity just holds the match data and links to the battle session.
 */
@Entity
@Table(name = "pvp_matches")
public class PvpMatchEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "player1_username", nullable = false)
    private String player1Username;

    @Column(name = "player2_username", nullable = false)
    private String player2Username;

    @Column(name = "player1_party_name", nullable = false)
    private String player1PartyName;

    @Column(name = "player2_party_name", nullable = false)
    private String player2PartyName;

    @Column(name = "battle_session_id")
    private String battleSessionId;

    @Column(name = "winner_username")
    private String winnerUsername;

    @Column(nullable = false)
    private boolean finished = false;

    protected PvpMatchEntity() {}

    public PvpMatchEntity(String id, String p1, String p1Party, String p2, String p2Party) {
        this.id = id;
        this.player1Username = p1;
        this.player1PartyName = p1Party;
        this.player2Username = p2;
        this.player2PartyName = p2Party;
    }

    public String getId() { return id; }
    public String getPlayer1Username() { return player1Username; }
    public String getPlayer2Username() { return player2Username; }
    public String getPlayer1PartyName() { return player1PartyName; }
    public String getPlayer2PartyName() { return player2PartyName; }
    public String getBattleSessionId() { return battleSessionId; }
    public String getWinnerUsername() { return winnerUsername; }
    public boolean isFinished() { return finished; }
    public void setBattleSessionId(String id) { this.battleSessionId = id; }
    public void setWinnerUsername(String winner) { this.winnerUsername = winner; }
    public void setFinished(boolean finished) { this.finished = finished; }
}

package org.example.lsw.pvp;

import org.example.lsw.core.Party;

/**
 * Request body for posting /api/pvp/match/start.
 * Contains both players and their chosen parties.
 */
public class StartPvpRequest {
    private String player1Username;
    private String player2Username;
    private Party player1Party;
    private Party player2Party;

    public StartPvpRequest() {}
    public String getPlayer1Username() { return player1Username; }
    public void setPlayer1Username(String u) { this.player1Username = u; }
    public String getPlayer2Username() { return player2Username; }
    public void setPlayer2Username(String u) { this.player2Username = u; }
    public Party getPlayer1Party() { return player1Party; }
    public void setPlayer1Party(Party p) { this.player1Party = p; }
    public Party getPlayer2Party() { return player2Party; }
    public void setPlayer2Party(Party p) { this.player2Party = p; }
}

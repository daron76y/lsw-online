package org.example.lsw.pvp;

/**
 * Response for pvp match calls
 * Contains match data plus the battle session id for the client
 */
public class PvpMatchResponse {
    private String matchId;
    private String player1Username;
    private String player2Username;
    private String player1PartyName;
    private String player2PartyName;
    private String battleSessionId;
    private boolean finished;
    private String winnerUsername;

    public PvpMatchResponse() {}

    public static PvpMatchResponse from(PvpMatchEntity e) {
        PvpMatchResponse r = new PvpMatchResponse();
        r.matchId          = e.getId();
        r.player1Username  = e.getPlayer1Username();
        r.player2Username  = e.getPlayer2Username();
        r.player1PartyName = e.getPlayer1PartyName();
        r.player2PartyName = e.getPlayer2PartyName();
        r.battleSessionId  = e.getBattleSessionId();
        r.finished         = e.isFinished();
        r.winnerUsername   = e.getWinnerUsername();
        return r;
    }

    public String getMatchId()                           { return matchId; }
    public void setMatchId(String matchId)               { this.matchId = matchId; }
    public String getPlayer1Username()                   { return player1Username; }
    public void setPlayer1Username(String u)             { this.player1Username = u; }
    public String getPlayer2Username()                   { return player2Username; }
    public void setPlayer2Username(String u)             { this.player2Username = u; }
    public String getPlayer1PartyName()                  { return player1PartyName; }
    public void setPlayer1PartyName(String n)            { this.player1PartyName = n; }
    public String getPlayer2PartyName()                  { return player2PartyName; }
    public void setPlayer2PartyName(String n)            { this.player2PartyName = n; }
    public String getBattleSessionId()                   { return battleSessionId; }
    public void setBattleSessionId(String id)            { this.battleSessionId = id; }
    public boolean isFinished()                          { return finished; }
    public void setFinished(boolean finished)            { this.finished = finished; }
    public String getWinnerUsername()                    { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
}

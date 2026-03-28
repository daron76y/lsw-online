package org.example.lsw.battle;

import org.example.lsw.core.Party;
import org.example.lsw.core.battle.BattleState;
import java.util.List;

/**
 * Response returned after every battle action and on GET /api/battle/{id}.
 * Contains everything the client needs to show to the battle screen/console.
 * Better than BattleState for sending over HTTP because it just contains
 * the essential info from the battle. BattleState is more complex and is
 * better suited for internal usage.
 * Hence, BattleStateRESPONSE (BattleState but suited for HTTP responses!)
 */
public class BattleStateResponse {
    private String battleId; //battle UUID
    private Party partyA;
    private Party partyB;
    private String currentTurnUnitName;
    private boolean finished;
    private String winnerPartyName; //null if battle not finished
    private List<String> messages; //output from this action so it can be displayed in the console

    public BattleStateResponse() {} //for Jackson

    public BattleStateResponse(String battleId, BattleState state) {
        this.battleId            = battleId;
        this.partyA              = state.getPartyA();
        this.partyB              = state.getPartyB();
        this.currentTurnUnitName = state.getCurrentTurnUnitName();
        this.finished            = state.isFinished();
        this.winnerPartyName     = state.getWinnerPartyName();
        this.messages            = state.getMessages();
    }

    //accessor methods for Jackson
    public String getBattleId()               { return battleId; }
    public void setBattleId(String battleId)  { this.battleId = battleId; }
    public Party getPartyA()                  { return partyA; }
    public void setPartyA(Party p)            { this.partyA = p; }
    public Party getPartyB()                  { return partyB; }
    public void setPartyB(Party p)            { this.partyB = p; }
    public String getCurrentTurnUnitName()                      { return currentTurnUnitName; }
    public void setCurrentTurnUnitName(String currentTurnUnitName) { this.currentTurnUnitName = currentTurnUnitName; }
    public boolean isFinished()               { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public String getWinnerPartyName()                    { return winnerPartyName; }
    public void setWinnerPartyName(String winnerPartyName){ this.winnerPartyName = winnerPartyName; }
    public List<String> getMessages()                     { return messages; }
    public void setMessages(List<String> messages)        { this.messages = messages; }
}

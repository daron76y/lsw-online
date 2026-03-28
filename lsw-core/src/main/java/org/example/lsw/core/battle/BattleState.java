package org.example.lsw.core.battle;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of a battle stored in the database between http requests
 * Instead of the BattleEngine storing state internally, it saves its data to a BattleState.
 */
@JsonAutoDetect(
    fieldVisibility    = JsonAutoDetect.Visibility.ANY,
    getterVisibility   = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class BattleState {
    private Party partyA;
    private Party partyB;
    private List<String> turnOrder = new ArrayList<>();
    private String currentTurnUnitName;
    private boolean finished = false;
    private String winnerPartyName;
    private List<String> messages = new ArrayList<>();

    private BattleState() {}

    public BattleState(Party partyA, Party partyB) {
        this.partyA = partyA;
        this.partyB = partyB;
    }

    public Party getPartyA()                         { return partyA; }
    public Party getPartyB()                         { return partyB; }
    public List<String> getTurnOrder()               { return turnOrder; }
    public String getCurrentTurnUnitName()           { return currentTurnUnitName; }
    public boolean isFinished()                      { return finished; }
    public String getWinnerPartyName()               { return winnerPartyName; }
    public List<String> getMessages()                { return messages; }

    public void setTurnOrder(List<String> turnOrder) { this.turnOrder = turnOrder; }
    public void setCurrentTurnUnitName(String name)  { this.currentTurnUnitName = name; }
    public void setFinished(boolean finished)        { this.finished = finished; }
    public void setWinnerPartyName(String name)      { this.winnerPartyName = name; }
    public void setMessages(List<String> messages)   { this.messages = messages; }

    public void addMessage(String msg)               { this.messages.add(msg); }
    public void clearMessages()                      { this.messages.clear(); }

    public Unit findUnit(String name) {
        for (Unit u : partyA.getUnits()) if (u.getName().equalsIgnoreCase(name)) return u;
        for (Unit u : partyB.getUnits()) if (u.getName().equalsIgnoreCase(name)) return u;
        return null;
    }

    //get friendly part from unit
    public Party partyOf(Unit unit) {
        if (partyA.getUnits().contains(unit)) return partyA;
        if (partyB.getUnits().contains(unit)) return partyB;
        return null;
    }

    //get the opposing party of a unit
    public Party enemyPartyOf(Unit unit) {
        Party ally = partyOf(unit);
        return ally == null ? null : (ally == partyA ? partyB : partyA);
    }
}

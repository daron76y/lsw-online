package org.example.lsw.core;

import org.example.lsw.core.battle.BattleState;
import java.util.List;

/**
 * OutputService implementation that collects messages into a battleState's message list
 */
public class MessageCollector implements OutputService {
    private final BattleState state;

    public MessageCollector(BattleState state) { this.state = state; }

    //TODO: implement missing ones or get rid of them from the interface
    @Override public void showMessage(String message) {state.addMessage(message);}

    @Override public void showParty(List<Party> partyList) {}

    @Override public void announceTurn(Unit unit) {state.addMessage("It is " + unit.getName() + "'s turn!");}

    @Override public void showUnitBasic(Unit unit) {state.addMessage(unit.toString());}

    @Override public void showUnitAdvanced(Unit unit) {
        state.addMessage(unit.toString());
        state.addMessage("- classes: " + unit.getClassLevels());
        state.addMessage("- abilities: " + unit.getAbilities());
        state.addMessage("- effects: " + unit.getEffects());
    }

    @Override public void showInventory(Party party) {}

    @Override public void showItemShop() {}
}

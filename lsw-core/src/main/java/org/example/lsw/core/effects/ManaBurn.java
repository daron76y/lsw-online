package org.example.lsw.core.effects;

import org.example.lsw.core.OutputService;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

public class ManaBurn extends Effect {
    public ManaBurn() {
        super(-1); //infinite duration
    }

    @Override
    public String getName() {return "Mana Burn";}

    @Override
    public void onAttack(Unit attacker, Party allyParty, Unit target, Party enemyParty, OutputService output) {
        int burnAmount = (int)(target.getMaxMana() * 0.10);
        target.setMana(target.getMana() - burnAmount);
        output.showMessage(String.format("- %s burns away %d mana!", target.getName(), burnAmount));
    }

    @Override
    public boolean isExpired() {return false;}
}

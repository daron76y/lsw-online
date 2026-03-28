package org.example.lsw.core.effects;

import org.example.lsw.core.OutputService;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

import java.util.List;
import java.util.Random;

public class SneakAttack extends Effect {
    public SneakAttack() {
        super(-1); //sneak attack is infinite
    }

    @Override
    public String getName() {return "Sneak Attack";}

    @Override
    public void onAttack(Unit attacker, Party allyParty, Unit target, Party enemyParty, OutputService output) {
        if (Math.random() < 0.5) {
            List<Unit> enemies = enemyParty.getAliveUnits();
            if (enemies.isEmpty()) return;
            Unit extraTarget = enemies.get(new Random().nextInt(enemies.size()));
            int inflictedDamage = extraTarget.applyDamage(attacker.getAttack());
            output.showMessage(String.format("- %s sneak attacks %s for %d damage!", attacker.getName(), extraTarget.getName(), inflictedDamage));
        }
    }

    @Override
    public boolean isExpired() {return false;}
}
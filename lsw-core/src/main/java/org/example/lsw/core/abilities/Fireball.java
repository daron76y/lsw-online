package org.example.lsw.core.abilities;

import org.example.lsw.core.OutputService;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

public class Fireball extends Ability {
    private final int damageMultiplier;

    private Fireball() { super(0); this.damageMultiplier = 0; } //jackson

    public Fireball(int manaCost, int damageMultiplier) {
        super(manaCost);
        this.damageMultiplier = damageMultiplier; //double damage for sorcerer upgrade
    }

    @Override
    public String getName() {return "Fireball";}

    @Override
    public boolean requiresTarget() {return true;}

    @Override
    public void perform(Unit caster, Unit target, Party allyParty, Party enemyParty, OutputService output) {
        //get the neighboring enemies of the target (fireball is an AOE attack)
        int beforeIndex = enemyParty.getAliveUnits().indexOf(target) - 1;
        int afterIndex = enemyParty.getAliveUnits().indexOf(target) + 1;
        Unit beforeEnemy = (beforeIndex < 0) ? null : enemyParty.getAliveUnits().get(beforeIndex);
        Unit afterEnemy = (afterIndex >= enemyParty.getNumAliveUnits()) ? null : enemyParty.getAliveUnits().get(afterIndex);

        //damage target and neighbors, if they exist
        int damage = caster.getAttack() * damageMultiplier;
        int inflictedDamage = target.applyDamage(damage);
        output.showMessage(String.format("- inflicts %d damage to %s", inflictedDamage, target.getName()));

        if (beforeEnemy != null) {
            inflictedDamage = beforeEnemy.applyDamage(damage);
            output.showMessage(String.format("- inflicts %d damage to %s", inflictedDamage, beforeEnemy.getName()));
        }

        if (afterEnemy != null) {
            inflictedDamage = afterEnemy.applyDamage(damage);
            output.showMessage(String.format("- inflicts %d damage to %s", inflictedDamage, afterEnemy.getName()));
        }
    }
}

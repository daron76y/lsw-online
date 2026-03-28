package org.example.lsw.core.effects;

import org.example.lsw.core.OutputService;
import org.example.lsw.core.Unit;

public class FireShield extends Shield {
    private final double reflectPercent;

    private FireShield() { super(0); this.reflectPercent = 0; } //jackson

    public FireShield(int shieldAmount, double reflectPercent) {
        super(-1); //infinite duration fire shield
        this.reflectPercent = reflectPercent;
    }

    @Override
    public String getName() {return "Fire Shield";}

    @Override
    public int modifyDamage(Unit attacker, Unit target, int damage, OutputService output) {
        damage = super.modifyDamage(attacker, target, damage, output);
        int reflected = (int)(damage * reflectPercent);
        if (attacker != null && reflected > 0) {
            attacker.setHealth(attacker.getHealth() - reflected);
            output.showMessage(String.format("%s reflected %d damage!", target.getName(), reflected));
        }
        return damage;
    }
}

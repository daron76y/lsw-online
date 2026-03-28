package org.example.lsw.core.effects;

import org.example.lsw.core.OutputService;
import org.example.lsw.core.Unit;

public class Shield extends Effect {
    private int shieldAmount;

    private Shield() { super(-1); this.shieldAmount = 0; } //jackson

    public Shield(int shieldAmount) {
        super(-1); //shield effect is infinite
        this.shieldAmount = shieldAmount;
    }

    @Override
    public String getName() {return "Shield";}

    @Override
    public int modifyDamage(Unit attacker, Unit target, int damage, OutputService output) {
        if (shieldAmount <= 0) return damage;
        int absorbedDamage = Math.min(shieldAmount, damage);
        shieldAmount -= absorbedDamage;
        output.showMessage(String.format("%s shielded %d damage!", target.getName(), absorbedDamage));
        return damage - absorbedDamage;
    }

    @Override
    public boolean isExpired() {return shieldAmount <= 0;}
}

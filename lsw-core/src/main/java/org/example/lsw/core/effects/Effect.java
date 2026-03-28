package org.example.lsw.core.effects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.lsw.core.OutputService;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Shield.class,      name = "Shield"),
        @JsonSubTypes.Type(value = FireShield.class,  name = "FireShield"),
        @JsonSubTypes.Type(value = ManaBurn.class,    name = "ManaBurn"),
        @JsonSubTypes.Type(value = SneakAttack.class, name = "SneakAttack"),
        @JsonSubTypes.Type(value = Stunned.class,     name = "Stunned"),
})
@JsonAutoDetect(
        fieldVisibility    = JsonAutoDetect.Visibility.ANY,
        getterVisibility   = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public abstract class Effect {
    private int duration; //how many turns

    public Effect(int duration) {
        this.duration = duration;
    }

    public abstract String getName();
    public int getDuration() {return duration;}
    public void decrementDuration() {duration--;}
    public boolean isExpired() {return duration <= 0;}

    // optional overrides unique to each effect. Not all effects need to override/implement
    public void onAttack(Unit attacker, Party allyParty, Unit target, Party enemyParty, OutputService output) {}
    public int modifyDamage(Unit attacker, Unit target, int damage, OutputService output) {return damage;}
    public boolean preventsAction() {return false;}

    //to string
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}

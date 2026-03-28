package org.example.lsw.core.effects;

public class Stunned extends Effect {
    private Stunned() { super(1); } //jackson
    public Stunned(int duration) {
        super(duration);
    }

    @Override
    public String getName() {return "Stunned";}

    @Override
    public boolean preventsAction() {return true;}
}

package org.example.lsw.core.battle;

import org.example.lsw.core.MessageCollector;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;
import org.example.lsw.core.abilities.Ability;
import org.example.lsw.core.effects.Effect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * StatefulBattleEngine that has static methods, and only uses a BattleState to process battle logic
 * for both PVE campaigns and PVP matches, hence the name, STATEFUL-BattleEngine (because it uses BattleStates)
 */
public class StatefulBattleEngine {
    //      ┌──────────────────────────────────────────────────────────┐
    //      │                 initialize battle state                  │
    //      └──────────────────────────────────────────────────────────┘
    public static BattleState initialize(Party partyA, Party partyB) {
        BattleState state = new BattleState(partyA, partyB);

        List<Unit> allUnits = new ArrayList<>();
        allUnits.addAll(partyA.getUnits());
        allUnits.addAll(partyB.getUnits());
        allUnits.removeIf(Unit::isDead);
        allUnits.sort(Comparator.comparingInt(Unit::getLevel).reversed());

        List<String> order = new ArrayList<>();
        for (Unit u : allUnits) order.add(u.getName());
        state.setTurnOrder(order);

        //set first unit directly - do not call advanceToNextLiving()
        //cuz it would pop and re-append the first unit, skipping it to position 2.
        state.setCurrentTurnUnitName(order.isEmpty() ? null : order.getFirst());
        state.addMessage("Battle begins!");
        state.addMessage(formatParties(state));
        addTurnPrompt(state);
        return state;
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                  process battle actions                  │
    //      └──────────────────────────────────────────────────────────┘
    public static BattleState processAction(BattleState state, String action, String targetName, String abilityName) {
        state.clearMessages();
        if (state.isFinished()) { state.addMessage("Battle is already over."); return state; }

        //get current unit
        Unit current = state.findUnit(state.getCurrentTurnUnitName());
        if (current == null || current.isDead()) {
            advanceToNextLiving(state);
            addTurnPrompt(state);
            return state;
        }

        //get allies and enemies
        Party ally  = state.partyOf(current);
        Party enemy = state.enemyPartyOf(current);
        MessageCollector out = new MessageCollector(state);

        try {
            switch (action.toLowerCase().trim()) {
                case "attack" -> {
                    //check all units (including dead) to give a better error message
                    Unit target = enemy.getUnitByName(targetName);
                    if (target == null) {
                        //TODO: redundant dead target check
                        boolean isDead = enemy.getUnits().stream()
                                .anyMatch(u -> u.getName().equalsIgnoreCase(targetName));
                        if (isDead) throw new IllegalArgumentException(targetName + " is already dead.");
                        throw new IllegalArgumentException("No such target: " + targetName);
                    }
                    if (target.isDead()) throw new IllegalArgumentException(targetName + " is already dead.");

                    //get damage with modifiers from effects
                    int dmg = current.getAttack();
                    for (Effect e : new ArrayList<>(target.getEffects())) dmg = e.modifyDamage(current, target, dmg, out);
                    target.getEffects().removeIf(Effect::isExpired);

                    //apply the damage to the target with effect modifiers
                    dmg = target.applyDamage(dmg);
                    state.addMessage(current.getName() + " attacks " + target.getName() + " for " + dmg + " damage!");
                    for (Effect e : new ArrayList<>(current.getEffects())) e.onAttack(current, ally, target, enemy, out);
                    current.getEffects().removeIf(Effect::isExpired);
                }
                case "defend" -> {
                    current.setHealth(Math.min(current.getHealth() + 10, current.getMaxHealth()));
                    current.setMana(Math.min(current.getMana() + 5, current.getMaxMana()));
                    state.addMessage(current.getName() + " defends! (+10 HP, +5 MP)");
                }
                case "wait" -> state.addMessage(current.getName() + " waits.");
                case "ai" -> {
                    //process enemy AI inputs. This is automatically called whenever it's not a player-unit's turn
                    if (current.getHealth() < current.getMaxHealth() * 0.25) { //defend if less than 25% health
                        current.setHealth(Math.min(current.getHealth() + 10, current.getMaxHealth()));
                        current.setMana(Math.min(current.getMana() + 5, current.getMaxMana()));
                        state.addMessage(current.getName() + " defends! (+10 HP, +5 MP)");
                    }
                    else if (ThreadLocalRandom.current().nextInt(1, 5) == 1) { //1-5 chance of waiting
                        state.addMessage(current.getName() + " waits.");
                    }
                    else { //attack
                        Unit target = enemy.getAliveUnits().stream()
                                .min(Comparator.comparingInt(Unit::getHealth))
                                .orElse(null);
                        if (target != null) {
                            //get damage
                            int dmg = current.getAttack();
                            for (Effect e : new ArrayList<>(target.getEffects())) dmg = e.modifyDamage(current, target, dmg, out);
                            target.getEffects().removeIf(Effect::isExpired);

                            //apply damage
                            dmg = target.applyDamage(dmg);
                            state.addMessage(current.getName() + " attacks " + target.getName() + " for " + dmg + " damage!");
                            for (Effect e : new ArrayList<>(current.getEffects())) e.onAttack(current, ally, target, enemy, out);
                            current.getEffects().removeIf(Effect::isExpired);
                        }
                    }
                }
                case "cast" -> {
                    //get ability
                    Ability ability = current.getAbilityByName(abilityName);
                    if (ability == null) throw new IllegalArgumentException("No such ability: " + abilityName);

                    //ensure mana
                    if (current.getMana() < ability.getManaCost()) throw new IllegalArgumentException("Not enough mana!");

                    //get target
                    Unit target = null;
                    if (ability.requiresTarget()) {
                        target = enemy.getUnitByName(targetName);
                        if (target == null) throw new IllegalArgumentException("No such target: " + targetName);
                        if (target.isDead()) throw new IllegalArgumentException(targetName + " is already dead.");
                    }

                    //cast the ability
                    current.setMana(current.getMana() - ability.getManaCost());
                    state.addMessage(current.getName() + " casts " + ability.getName() + "!");
                    ability.perform(current, target, ally, enemy, out);
                }
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            state.addMessage("Error: " + e.getMessage());
            addTurnPrompt(state);
            return state;
        }

        //decrement the duration of effects for unit that just acted
        current.getEffects().forEach(Effect::decrementDuration);
        current.getEffects().removeIf(Effect::isExpired);

        if (isGameOver(state)) return state;

        advanceToNextLiving(state);

        //skip stunned units
        while (!state.isFinished()) {
            Unit next = state.findUnit(state.getCurrentTurnUnitName());
            if (next == null || next.isDead()) {advanceToNextLiving(state); continue;}
            if (next.getEffects().stream().anyMatch(Effect::preventsAction)) {
                state.addMessage(next.getName() + " is stunned and loses their turn!!");
                next.getEffects().forEach(Effect::decrementDuration);
                next.getEffects().removeIf(Effect::isExpired);
                if (isGameOver(state)) return state;
                advanceToNextLiving(state);
            } else break;
        }

        //fully finish this turn and remove the resultant state
        if (!state.isFinished()) {
            state.addMessage(formatParties(state));
            addTurnPrompt(state);
        }
        return state;
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                  next living units turn                  │
    //      └──────────────────────────────────────────────────────────┘
    private static void advanceToNextLiving(BattleState state) {
        List<String> queue = state.getTurnOrder();
        queue.removeIf(name -> {Unit u = state.findUnit(name); return u == null || u.isDead();});
        if (queue.isEmpty()) {state.setCurrentTurnUnitName(null); return;}
        String first = queue.removeFirst();
        Unit u = state.findUnit(first);
        if (u != null && u.isAlive()) queue.add(first);
        queue.removeIf(name -> {Unit v = state.findUnit(name); return v == null || v.isDead();});
        state.setCurrentTurnUnitName(queue.isEmpty() ? null : queue.getFirst());
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                     check game over                      │
    //      └──────────────────────────────────────────────────────────┘
    private static boolean isGameOver(BattleState state) {
        if (state.getPartyA().isDefeated()) {
            state.setFinished(true);
            state.setWinnerPartyName(state.getPartyB().getName());
            state.addMessage("Battle over! " + state.getPartyB().getName() + " wins!");
            cleanup(state);
            return true;
        }
        if (state.getPartyB().isDefeated()) {
            state.setFinished(true);
            state.setWinnerPartyName(state.getPartyA().getName());
            state.addMessage("Battle over! " + state.getPartyA().getName() + " wins!");
            cleanup(state);
            return true;
        }
        return false;
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                     //clear debuffs                      │
    //      └──────────────────────────────────────────────────────────┘
    private static void cleanup(BattleState state) {
        state.getPartyA().getUnits().forEach(Unit::clearDebuffEffects);
        state.getPartyB().getUnits().forEach(Unit::clearDebuffEffects);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                   turn prompt message                    │
    //      └──────────────────────────────────────────────────────────┘
    private static void addTurnPrompt(BattleState state) {
        Unit u = state.findUnit(state.getCurrentTurnUnitName());
        if (u == null) return;
        state.addMessage("It is " + u.getName() + "'s turn!");
        state.addMessage("Actions: [attack <target>] [defend] [wait] [cast \"<ability>\" <target>]");
        state.addMessage("Abilities: " + u.getAbilities().stream()
                .map(a -> a.getName() + "(" + a.getManaCost() + "mp)").toList());
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                   party status message                   │
    //      └──────────────────────────────────────────────────────────┘
    private static String formatParties(BattleState state) {
        StringBuilder sb = new StringBuilder("─────────────────────────────\n");
        for (Unit u : state.getPartyA().getUnits())
            sb.append(u.isAlive()
                    ? String.format("  %-12s ATK:%d DEF:%d HP:%d/%d MP:%d/%d [%s]%n",
                    u.getName(),
                    u.getAttack(),
                    u.getDefense(),
                    u.getHealth(),
                    u.getMaxHealth(),
                    u.getMana(),
                    u.getMaxMana(),
                    u.getMainClass())
                    : String.format("  %-12s [DEAD]%n", u.getName()));
        sb.append("  vs\n");
        for (Unit u : state.getPartyB().getUnits())
            sb.append(u.isAlive()
                    ? String.format("  %-12s ATK:%d DEF:%d HP:%d/%d MP:%d/%d [%s]%n",
                    u.getName(),
                    u.getAttack(),
                    u.getDefense(),
                    u.getHealth(),
                    u.getMaxHealth(),
                    u.getMana(),
                    u.getMaxMana(),
                    u.getMainClass())
                    : String.format("  %-12s [DEAD]%n", u.getName()));
        sb.append("─────────────────────────────");
        return sb.toString();
    }
}
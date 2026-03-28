package org.example.lsw.battle;

import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;
import org.example.lsw.core.battle.BattleState;
import org.example.lsw.core.battle.StatefulBattleEngine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class BattleService {
    private final BattleSessionRepository repo; //repo for storing info on the battle sessions

    public BattleService(BattleSessionRepository repo) {
        this.repo = repo;
    }

    /**
     * Creates a new battle session from two parties
     * initializes turn order and opening messages using the StatefulBattleEngine
     */
    public BattleStateResponse startBattle(StartBattleRequest req) {
        //create a unique id for this battle
        String id = UUID.randomUUID().toString();

        //create a BattleState with initial info
        BattleState state = StatefulBattleEngine.initialize(req.getPartyA(), req.getPartyB());

        //create the session entity to store in the database, and save it
        BattleSessionEntity entity = new BattleSessionEntity(id, req.getOwnerId(), req.getOwnerType(), state);
        repo.save(entity);

        return new BattleStateResponse(id, state);
    }

    /**
     * Returns the current battle state, cleanly formatted
     */
    public BattleStateResponse getState(String battleId) {
        BattleSessionEntity entity = getOrThrow(battleId);
        BattleState state = entity.getState();

        //return a formatted snapshot so the client can display the current party stats
        state.clearMessages();
        state.addMessage(formatParties(state));
        addTurnPrompt(state);

        return new BattleStateResponse(battleId, state);
    }

    /**
     * Formats the party listings to show in the console after each turn in a battle
     */
    private static String formatParties(BattleState state) {
        StringBuilder sb = new StringBuilder("─────────────────────────────\n");
        for (org.example.lsw.core.Unit u : state.getPartyA().getUnits()) //partyA
            sb.append(u.isAlive()
                    ? String.format("  %-12s ATK:%d DEF:%d HP:%d/%d MP:%d/%d [%s]%n",
                    u.getName(), u.getAttack(), u.getDefense(),
                    u.getHealth(), u.getMaxHealth(), u.getMana(), u.getMaxMana(),
                    u.getMainClass())
                    : String.format("  %-12s [DEAD]%n", u.getName()));
        sb.append("  vs\n");
        for (org.example.lsw.core.Unit u : state.getPartyB().getUnits()) //partyB
            sb.append(u.isAlive()
                    ? String.format("  %-12s ATK:%d DEF:%d HP:%d/%d MP:%d/%d [%s]%n",
                    u.getName(), u.getAttack(), u.getDefense(),
                    u.getHealth(), u.getMaxHealth(), u.getMana(), u.getMaxMana(),
                    u.getMainClass())
                    : String.format("  %-12s [DEAD]%n", u.getName()));
        sb.append("─────────────────────────────");
        return sb.toString();
    }

    private static void addTurnPrompt(BattleState state) {
        //get the current unit whose turn it is in the battle
        Unit u = state.getPartyA().getUnits().stream()
                .filter(x -> x.getName()
                .equals(state.getCurrentTurnUnitName()))
                .findFirst()
                .orElse(state.getPartyB().getUnits().stream()
                        .filter(x -> x.getName().equals(state.getCurrentTurnUnitName()))
                        .findFirst().orElse(null));
        if (u == null) return;

        //turn prompts for the unit
        state.addMessage("It is " + u.getName() + "'s turn!");
        state.addMessage("Actions: [attack <target>] [defend] [wait] [cast \"<ability>\" <target>]");
        state.addMessage("Abilities: " + u.getAbilities().stream()
                .map(a -> a.getName() + "(" + a.getManaCost() + "mp)").toList());
    }

    /**
     * Processes one player action for the current units turn. Called for every turn.
     * Loads state from the database, runs the action, saves updated state, returns result
     */
    public BattleStateResponse processAction(String battleId, BattleActionRequest req) {
        BattleSessionEntity entity = getOrThrow(battleId); //get the battle session

        //ensure we're not trying to process a completed battle
        if (entity.isFinished())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Battle is already finished.");

        //get the state of the battle and update it with the requested action parameters
        BattleState state = entity.getState();
        state = StatefulBattleEngine.processAction(
                state,
                req.getAction(),
                req.getTargetName(),
                req.getAbilityName()
        );

        //update the battle session with the new modified state and save it to the repo
        entity.updateState(state);
        repo.save(entity);

        //return a response on success
        return new BattleStateResponse(battleId, state);
    }

    /**
     * Returns the final state of a finished battle
     * Also revives both parties so they can be saved back to whoever called this.
     */
    public BattleStateResponse getFinishedResult(String battleId) {
        //get the battle session
        BattleSessionEntity entity = getOrThrow(battleId);

        //ensure we only get a finished result, from a finished battle. (Duh)
        if (!entity.isFinished())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Battle is not finished yet.");

        //get the state of the battle
        BattleState state = entity.getState();

        //revive both parties so they can be saved and reused by pvp-service, since pvp doesn't have item usage
        reviveParty(state.getPartyA());
        reviveParty(state.getPartyB());

        return new BattleStateResponse(battleId, state);
    }

    /**
     * Deletes a battle session once it has ended.
     */
    public void deleteBattle(String battleId) {
        repo.deleteById(battleId);
    }

    //HELPERS ============================================================

    private BattleSessionEntity getOrThrow(String battleId) {
        return repo.findById(battleId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Battle session not found: " + battleId
        ));
    }

    private void reviveParty(Party party) {
        for (Unit unit : party.getUnits()) {
            unit.setHealth(unit.getMaxHealth());
            unit.setMana(unit.getMaxMana());
        }
    }
}
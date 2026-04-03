package org.example.lsw.battle;

import org.example.lsw.core.HeroClass;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;
import org.example.lsw.core.battle.BattleState;
import org.example.lsw.core.battle.StatefulBattleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatefulBattleEngineTest {

    private Party partyA;
    private Party partyB;

    @BeforeEach
    void setUp() {
        partyA = new Party("Heroes");
        partyA.addUnit(new Unit("Hero1", 10, 5, 100, 50, HeroClass.WARRIOR));
        partyA.addUnit(new Unit("Hero2", 8, 3, 80, 60, HeroClass.MAGE));

        partyB = new Party("Enemies");
        partyB.addUnit(new Unit("Enemy1", 12, 4, 90, 40, HeroClass.WARRIOR));
        partyB.addUnit(new Unit("Enemy2", 6, 6, 120, 30, HeroClass.ORDER));
    }

    @Test
    void testInitialize() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);

        assertNotNull(state);
        assertFalse(state.isFinished());
        assertNotNull(state.getCurrentTurnUnitName());
        assertFalse(state.getTurnOrder().isEmpty());
        assertFalse(state.getMessages().isEmpty());
    }

    @Test
    void testAttackAction() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);
        String currentUnit = state.getCurrentTurnUnitName();

        // Find a target from the enemy party
        Unit target = state.partyOf(state.findUnit(currentUnit)) == partyA
                ? partyB.getAliveUnits().get(0)
                : partyA.getAliveUnits().get(0);

        int initialHealth = target.getHealth();

        state = StatefulBattleEngine.processAction(
                state, "attack", target.getName(), null
        );

        assertTrue(target.getHealth() < initialHealth);
        assertFalse(state.getMessages().isEmpty());
    }

    @Test
    void testDefendAction() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);
        Unit currentUnit = state.findUnit(state.getCurrentTurnUnitName());

        int initialHealth = currentUnit.getHealth();
        int initialMana = currentUnit.getMana();

        state = StatefulBattleEngine.processAction(state, "defend", null, null);

        assertTrue(currentUnit.getHealth() >= initialHealth);
        assertTrue(currentUnit.getMana() >= initialMana);
    }

    @Test
    void testWaitAction() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);
        String initialTurn = state.getCurrentTurnUnitName();

        state = StatefulBattleEngine.processAction(state, "wait", null, null);

        assertNotEquals(initialTurn, state.getCurrentTurnUnitName());
    }

    @Test
    void testInvalidAction() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);

        state = StatefulBattleEngine.processAction(
                state, "invalid", null, null
        );

        assertTrue(state.getMessages().stream()
                .anyMatch(m -> m.contains("Error") || m.contains("Unknown")));
    }

    @Test
    void testBattleCompletion() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);

        // Kill all units in partyB
        partyB.getUnits().forEach(u -> u.setHealth(0));

        // Process any action to trigger win condition check
        state = StatefulBattleEngine.processAction(state, "wait", null, null);

        assertTrue(state.isFinished());
        assertNotNull(state.getWinnerPartyName());
        assertEquals("Heroes", state.getWinnerPartyName());
    }

    @Test
    void testTurnOrderProgression() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);
        String firstUnit = state.getCurrentTurnUnitName();

        state = StatefulBattleEngine.processAction(state, "wait", null, null);
        String secondUnit = state.getCurrentTurnUnitName();

        assertNotEquals(firstUnit, secondUnit);
        assertTrue(state.getTurnOrder().contains(firstUnit));
    }

    @Test
    void testDeadUnitsSkipped() {
        BattleState state = StatefulBattleEngine.initialize(partyA, partyB);

        // Kill a unit
        Unit toKill = partyA.getUnits().get(0);
        toKill.setHealth(0);

        // Process several turns
        for (int i = 0; i < 10; i++) {
            state = StatefulBattleEngine.processAction(state, "wait", null, null);
            assertNotEquals(toKill.getName(), state.getCurrentTurnUnitName());
        }
    }
}
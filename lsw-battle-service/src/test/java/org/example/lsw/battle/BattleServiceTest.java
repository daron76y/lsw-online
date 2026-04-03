package org.example.lsw.battle;

import org.example.lsw.core.HeroClass;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;
import org.example.lsw.core.battle.BattleState;
import org.example.lsw.core.battle.StatefulBattleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BattleServiceTest {

    @Mock
    private BattleSessionRepository repo;

    @InjectMocks
    private BattleService service;

    private Party partyA;
    private Party partyB;

    @BeforeEach
    void setUp() {
        partyA = new Party("Party A");
        partyA.addUnit(new Unit("Hero1", HeroClass.WARRIOR));

        partyB = new Party("Party B");
        partyB.addUnit(new Unit("Enemy1", HeroClass.WARRIOR));
    }

    @Test
    void testStartBattle() {
        StartBattleRequest request = new StartBattleRequest();
        request.setOwnerId("test-owner");
        request.setOwnerType("PVE");
        request.setPartyA(partyA);
        request.setPartyB(partyB);

        when(repo.save(any(BattleSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        BattleStateResponse response = service.startBattle(request);

        assertNotNull(response);
        assertNotNull(response.getBattleId());
        assertFalse(response.isFinished());
        verify(repo, times(1)).save(any(BattleSessionEntity.class));
    }

    @Test
    void testGetState() {
        String battleId = "test-battle-id";
        BattleState state = new BattleState(partyA, partyB);
        state.setCurrentTurnUnitName("Hero1");

        BattleSessionEntity entity = new BattleSessionEntity(
                battleId, "owner", "PVE", state
        );

        when(repo.findById(battleId)).thenReturn(Optional.of(entity));

        BattleStateResponse response = service.getState(battleId);

        assertNotNull(response);
        assertEquals(battleId, response.getBattleId());
        assertFalse(response.getMessages().isEmpty());
    }

    @Test
    void testGetStateNotFound() {
        when(repo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            service.getState("nonexistent");
        });
    }

    @Test
    void testProcessAction() {
        String battleId = "test-battle-id";
        BattleState state = new BattleState(partyA, partyB);
        state.setCurrentTurnUnitName("Hero1");

        BattleSessionEntity entity =
                new BattleSessionEntity(battleId, "owner", "PVE", state);

        when(repo.findById(battleId)).thenReturn(Optional.of(entity));
        when(repo.save(any(BattleSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        BattleActionRequest request = new BattleActionRequest();
        request.setAction("defend");

        BattleState mockedResult = new BattleState(partyA, partyB);

        try (MockedStatic<StatefulBattleEngine> mockedEngine =
                     mockStatic(StatefulBattleEngine.class)) {

            mockedEngine.when(() ->
                    StatefulBattleEngine.processAction(
                            any(BattleState.class),
                            eq("defend"),
                            any(),
                            any()
                    )
            ).thenReturn(mockedResult);

            BattleStateResponse response =
                    service.processAction(battleId, request);

            assertNotNull(response);
            verify(repo, times(1)).save(any(BattleSessionEntity.class));
        }
    }

    @Test
    void testProcessActionOnFinishedBattle() {
        String battleId = "test-battle-id";
        BattleState state = new BattleState(partyA, partyB);
        state.setFinished(true);

        BattleSessionEntity entity = new BattleSessionEntity(
                battleId, "owner", "PVE", state
        );

        when(repo.findById(battleId)).thenReturn(Optional.of(entity));

        BattleActionRequest request = new BattleActionRequest();
        request.setAction("attack");
        request.setTargetName("Enemy1");

        assertThrows(ResponseStatusException.class, () -> {
            service.processAction(battleId, request);
        });
    }

    @Test
    void testGetFinishedResult() {
        String battleId = "test-battle-id";
        BattleState state = new BattleState(partyA, partyB);
        state.setFinished(true);
        state.setWinnerPartyName("Party A");

        // Kill all units in partyB
        partyB.getUnits().forEach(u -> u.setHealth(0));

        BattleSessionEntity entity = new BattleSessionEntity(
                battleId, "owner", "PVE", state
        );

        when(repo.findById(battleId)).thenReturn(Optional.of(entity));

        BattleStateResponse response = service.getFinishedResult(battleId);

        assertNotNull(response);
        assertTrue(response.isFinished());

        // All units should be revived
        assertTrue(response.getPartyA().getUnits().stream()
                .allMatch(u -> u.getHealth() == u.getMaxHealth()));
        assertTrue(response.getPartyB().getUnits().stream()
                .allMatch(u -> u.getHealth() == u.getMaxHealth()));
    }

    @Test
    void testDeleteBattle() {
        String battleId = "test-battle-id";

        doNothing().when(repo).deleteById(battleId);

        assertDoesNotThrow(() -> service.deleteBattle(battleId));
        verify(repo, times(1)).deleteById(battleId);
    }
}
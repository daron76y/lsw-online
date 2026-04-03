package org.example.lsw.pvp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.HeroClass;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PvpServiceTest {

    @Mock
    private PvpMatchRepository repo;

    @Mock
    private BattleServiceClient battleClient;

    @Mock
    private UserServiceClient userClient;

    @InjectMocks
    private PvpService service;

    private Party player1Party;
    private Party player2Party;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = GameMapper.get();

        player1Party = new Party("P1 Party");
        player1Party.addUnit(new Unit("P1Hero", HeroClass.WARRIOR));

        player2Party = new Party("P2 Party");
        player2Party.addUnit(new Unit("P2Hero", HeroClass.MAGE));
    }

    @Test
    void testStartMatch() {
        StartPvpRequest request = new StartPvpRequest();
        request.setPlayer1Username("player1");
        request.setPlayer2Username("player2");
        request.setPlayer1Party(player1Party);
        request.setPlayer2Party(player2Party);

        when(battleClient.startBattle(any(), any(), any()))
                .thenReturn("battle-id-123");
        when(repo.save(any(PvpMatchEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        PvpMatchResponse response = service.startMatch(request);

        assertNotNull(response);
        assertNotNull(response.getMatchId());
        assertEquals("battle-id-123", response.getBattleSessionId());
        assertEquals("player1", response.getPlayer1Username());
        assertEquals("player2", response.getPlayer2Username());
        assertFalse(response.isFinished());

        verify(battleClient, times(1)).startBattle(any(), any(), any());
        verify(repo, times(1)).save(any(PvpMatchEntity.class));
    }

    @Test
    void testGetMatch() {
        PvpMatchEntity entity = new PvpMatchEntity(
                "match-id",
                "player1", "P1 Party",
                "player2", "P2 Party"
        );
        entity.setBattleSessionId("battle-id");

        when(repo.findById("match-id")).thenReturn(Optional.of(entity));

        PvpMatchResponse response = service.getMatch("match-id");

        assertNotNull(response);
        assertEquals("match-id", response.getMatchId());
        assertEquals("player1", response.getPlayer1Username());
        assertEquals("battle-id", response.getBattleSessionId());
    }

    @Test
    void testGetMatchNotFound() {
        when(repo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            service.getMatch("nonexistent");
        });
    }

    @Test
    void testResolveMatchPlayer1Wins() {
        PvpMatchEntity entity = new PvpMatchEntity(
                "match-id",
                "player1", "P1 Party",
                "player2", "P2 Party"
        );
        entity.setBattleSessionId("battle-id");

        // Create battle result JSON
        ObjectNode battleResult = mapper.createObjectNode();
        battleResult.put("winnerPartyName", "P1 Party");
        battleResult.set("partyA", mapper.valueToTree(player1Party));
        battleResult.set("partyB", mapper.valueToTree(player2Party));

        when(repo.findById("match-id")).thenReturn(Optional.of(entity));
        when(battleClient.getBattleResult("battle-id")).thenReturn(battleResult);
        when(repo.save(any(PvpMatchEntity.class)))
                .thenAnswer(i -> i.getArgument(0));
        doNothing().when(battleClient).deleteBattle("battle-id");
        doNothing().when(userClient).recordPvpResult(any(), anyBoolean());
        doNothing().when(userClient).replacePvpParty(any(), anyInt(), any());

        PvpMatchResponse response = service.resolveMatch("match-id");

        assertTrue(response.isFinished());
        assertEquals("player1", response.getWinnerUsername());

        verify(userClient, times(1)).recordPvpResult("player1", true);
        verify(userClient, times(1)).recordPvpResult("player2", false);
        verify(battleClient, times(1)).deleteBattle("battle-id");
    }

    @Test
    void testResolveMatchPlayer2Wins() {
        PvpMatchEntity entity = new PvpMatchEntity(
                "match-id",
                "player1", "P1 Party",
                "player2", "P2 Party"
        );
        entity.setBattleSessionId("battle-id");

        ObjectNode battleResult = mapper.createObjectNode();
        battleResult.put("winnerPartyName", "P2 Party");
        battleResult.set("partyA", mapper.valueToTree(player1Party));
        battleResult.set("partyB", mapper.valueToTree(player2Party));

        when(repo.findById("match-id")).thenReturn(Optional.of(entity));
        when(battleClient.getBattleResult("battle-id")).thenReturn(battleResult);
        when(repo.save(any(PvpMatchEntity.class)))
                .thenAnswer(i -> i.getArgument(0));
        doNothing().when(battleClient).deleteBattle("battle-id");
        doNothing().when(userClient).recordPvpResult(any(), anyBoolean());
        doNothing().when(userClient).replacePvpParty(any(), anyInt(), any());

        PvpMatchResponse response = service.resolveMatch("match-id");

        assertTrue(response.isFinished());
        assertEquals("player2", response.getWinnerUsername());

        verify(userClient, times(1)).recordPvpResult("player1", false);
        verify(userClient, times(1)).recordPvpResult("player2", true);
    }

    @Test
    void testResolveMatchAlreadyFinished() {
        PvpMatchEntity entity = new PvpMatchEntity(
                "match-id",
                "player1", "P1 Party",
                "player2", "P2 Party"
        );
        entity.setBattleSessionId("battle-id");
        entity.setFinished(true);

        when(repo.findById("match-id")).thenReturn(Optional.of(entity));

        assertThrows(ResponseStatusException.class, () -> {
            service.resolveMatch("match-id");
        });

        verify(battleClient, never()).getBattleResult(any());
    }

    @Test
    void testResolveMatchSavesPartiesBackToRoster() {
        PvpMatchEntity entity = new PvpMatchEntity(
                "match-id",
                "player1", "P1 Party",
                "player2", "P2 Party"
        );
        entity.setBattleSessionId("battle-id");

        ObjectNode battleResult = mapper.createObjectNode();
        battleResult.put("winnerPartyName", "P1 Party");
        battleResult.set("partyA", mapper.valueToTree(player1Party));
        battleResult.set("partyB", mapper.valueToTree(player2Party));

        when(repo.findById("match-id")).thenReturn(Optional.of(entity));
        when(battleClient.getBattleResult("battle-id")).thenReturn(battleResult);
        when(repo.save(any(PvpMatchEntity.class)))
                .thenAnswer(i -> i.getArgument(0));
        doNothing().when(battleClient).deleteBattle("battle-id");
        doNothing().when(userClient).recordPvpResult(any(), anyBoolean());
        doNothing().when(userClient).replacePvpParty(any(), anyInt(), any());

        service.resolveMatch("match-id");

        verify(userClient, times(1)).replacePvpParty(eq("player1"), eq(0), any(Party.class));
        verify(userClient, times(1)).replacePvpParty(eq("player2"), eq(0), any(Party.class));
    }

    @Test
    void testGetActiveMatches() {
        PvpMatchEntity active1 = new PvpMatchEntity(
                "match-1", "p1", "Party1", "p2", "Party2"
        );

        PvpMatchEntity active2 = new PvpMatchEntity(
                "match-2", "p3", "Party3", "p4", "Party4"
        );

        PvpMatchEntity finished = new PvpMatchEntity(
                "match-3", "p5", "Party5", "p6", "Party6"
        );
        finished.setFinished(true);

        when(repo.findAll()).thenReturn(List.of(active1, active2, finished));

        List<PvpMatchResponse> activeMatches = service.getActiveMatches();

        assertEquals(2, activeMatches.size());
        assertTrue(activeMatches.stream().noneMatch(PvpMatchResponse::isFinished));
    }

    @Test
    void testGetActiveMatchesEmpty() {
        when(repo.findAll()).thenReturn(List.of());

        List<PvpMatchResponse> activeMatches = service.getActiveMatches();

        assertTrue(activeMatches.isEmpty());
    }
}
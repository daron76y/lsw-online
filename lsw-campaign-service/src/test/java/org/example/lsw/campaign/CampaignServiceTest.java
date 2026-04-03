package org.example.lsw.campaign;

import org.example.lsw.core.*;
import org.example.lsw.core.battle.BattleState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignSessionRepository repo;

    @Mock
    private BattleServiceClient battleClient;

    @InjectMocks
    private CampaignService service;

    private Party testParty;
    private CampaignState testState;

    @BeforeEach
    void setUp() {
        testParty = new Party("Test Party");
        testParty.addUnit(new Unit("Hero", HeroClass.WARRIOR));
        testParty.setGold(500);

        testState = new CampaignState("session-id", "testuser", testParty);
    }

    @Test
    void testStartCampaign() {
        StartCampaignRequest request = new StartCampaignRequest();
        request.setUsername("testuser");
        request.setParty(testParty);

        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        CampaignStateResponse response = service.startCampaign(request);

        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertEquals(0, response.getCurrentRoom());
        assertFalse(response.getMessages().isEmpty());
        verify(repo, times(1)).save(any(CampaignSessionEntity.class));
    }

    @Test
    void testResumeCampaign() {
        StartCampaignRequest request = new StartCampaignRequest();
        request.setUsername("testuser");
        request.setParty(testParty);
        request.setStartRoom(10);

        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        CampaignStateResponse response = service.startCampaign(request);

        assertEquals(10, response.getCurrentRoom());
        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("resumed")));
    }

    @Test
    void testNextRoomBattle() {
        testState.setPhase(CampaignState.Phase.OVERWORLD);
        CampaignSessionEntity entity = new CampaignSessionEntity(testState);

        when(repo.findById("session-id")).thenReturn(Optional.of(entity));
        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        CampaignActionRequest request = new CampaignActionRequest();
        request.setAction("next");

        CampaignStateResponse response = service.processAction("session-id", request);

        assertTrue(response.getCurrentRoom() > 0);
    }

    @Test
    void testNextRoomInn() {
        testState.setPhase(CampaignState.Phase.OVERWORLD);
        testState.setCurrentRoom(0);

        // Force an inn by setting the room type
        testState.setRoomType(1, CampaignState.RoomType.INN);

        CampaignSessionEntity entity = new CampaignSessionEntity(testState);

        when(repo.findById("session-id")).thenReturn(Optional.of(entity));
        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        CampaignActionRequest request = new CampaignActionRequest();
        request.setAction("next");

        CampaignStateResponse response = service.processAction("session-id", request);

        assertEquals(CampaignState.Phase.INN, response.getPhase());
    }

    @Test
    void testBuyItem() {
        // Set phase and wrap state in entity
        testState.setPhase(CampaignState.Phase.INN);
        CampaignSessionEntity entity = new CampaignSessionEntity(testState);

        // Mock repository
        when(repo.findById("session-id")).thenReturn(Optional.of(entity));
        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Build request to buy bread
        CampaignActionRequest request = new CampaignActionRequest();
        request.setAction("buy");
        request.setItemName("BREAD");

        // Record initial gold from entity's state, not testParty
        int initialGold = entity.getState().getPlayerParty().getGold();

        // Process the action
        CampaignStateResponse response = service.processAction("session-id", request);

        // Get the updated state from the entity
        CampaignState updatedState = entity.getState();
        Party updatedParty = updatedState.getPlayerParty();

        // Assertions
        assertTrue(updatedParty.getGold() < initialGold, "Gold should decrease after buying an item");
        assertTrue(updatedParty.getInventory().get(Items.BREAD) > 0, "Inventory should contain the purchased item");
    }

    @Test
    void testBuyItemInsufficientGold() {
        testParty.setGold(10); // Not enough for most items
        testState.setPhase(CampaignState.Phase.INN);
        CampaignSessionEntity entity = new CampaignSessionEntity(testState);

        when(repo.findById("session-id")).thenReturn(Optional.of(entity));
        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        CampaignActionRequest request = new CampaignActionRequest();
        request.setAction("buy");
        request.setItemName("ELIXIR");

        CampaignStateResponse response = service.processAction("session-id", request);

        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("Not enough gold")));
    }

    @Test
    void testUseItem() {
        // Add bread to the party and set initial hero health
        testParty.addItem(Items.BREAD);
        Unit hero = testParty.getUnits().get(0);
        hero.setHealth(50);
        hero.applyDamage(10); // now health is 40 (or whatever your applyDamage does)
        int initialHealth = hero.getHealth();

        // Set campaign phase and wrap state in entity
        testState.setPhase(CampaignState.Phase.OVERWORLD);
        CampaignSessionEntity entity = new CampaignSessionEntity(testState);

        // Mock repository
        when(repo.findById("session-id")).thenReturn(Optional.of(entity));
        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Build request to use item
        CampaignActionRequest request = new CampaignActionRequest();
        request.setAction("use-item");
        request.setItemName("BREAD");
        request.setUnitName("Hero");

        // Process action
        CampaignStateResponse response = service.processAction("session-id", request);

        // Get the updated state from the entity
        CampaignState updatedState = entity.getState();
        Unit updatedHero = updatedState.getPlayerParty().getUnits().get(0);

        // Assert the hero's health increased
        assertTrue(updatedHero.getHealth() > initialHealth,
                "Hero's health should increase after using bread");
    }

    @Test
    void testFinishCampaign() {
        testState.setCurrentRoom(30); // One room before finish
        testState.setPhase(CampaignState.Phase.OVERWORLD);
        CampaignSessionEntity entity = new CampaignSessionEntity(testState);

        when(repo.findById("session-id")).thenReturn(Optional.of(entity));
        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        CampaignActionRequest request = new CampaignActionRequest();
        request.setAction("next");

        CampaignStateResponse response = service.processAction("session-id", request);

        assertTrue(response.isFinished());
        assertEquals(CampaignState.Phase.FINISHED, response.getPhase());
    }

    @Test
    void testViewParty() {
        testState.setPhase(CampaignState.Phase.OVERWORLD);
        CampaignSessionEntity entity = new CampaignSessionEntity(testState);

        when(repo.findById("session-id")).thenReturn(Optional.of(entity));
        when(repo.save(any(CampaignSessionEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        CampaignActionRequest request = new CampaignActionRequest();
        request.setAction("view-party");

        CampaignStateResponse response = service.processAction("session-id", request);

        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("Test Party") || m.contains("Hero")));
    }
}
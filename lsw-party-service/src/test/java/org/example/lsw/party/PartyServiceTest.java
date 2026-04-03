package org.example.lsw.party;

import org.example.lsw.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartyServiceTest {

    @InjectMocks
    private PartyService service;

    private Party testParty;
    private Unit testUnit;

    @BeforeEach
    void setUp() {
        testParty = new Party("Test Party");
        testUnit = new Unit("Hero", HeroClass.WARRIOR);
        testParty.addUnit(testUnit);
        testParty.setGold(1000);
    }

    @Test
    void testLevelUpSuccess() {
        testUnit.addExperience(10000);

        LevelUpRequest request = new LevelUpRequest();
        request.setParty(testParty);
        request.setUnitName("Hero");
        request.setHeroClass("WARRIOR");

        int initialLevel = testUnit.getLevel();

        PartyResponse response = service.levelUp(request);

        assertEquals(initialLevel + 1, testUnit.getLevel());
        assertFalse(response.getMessages().isEmpty());
        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("leveled up")));
    }

    @Test
    void testLevelUpInsufficientExperience() {
        LevelUpRequest request = new LevelUpRequest();
        request.setParty(testParty);
        request.setUnitName("Hero");
        request.setHeroClass("WARRIOR");

        assertThrows(ResponseStatusException.class, () -> {
            service.levelUp(request);
        });
    }

    @Test
    void testLevelUpUnknownClass() {
        testUnit.addExperience(10000);

        LevelUpRequest request = new LevelUpRequest();
        request.setParty(testParty);
        request.setUnitName("Hero");
        request.setHeroClass("INVALID_CLASS");

        assertThrows(ResponseStatusException.class, () -> {
            service.levelUp(request);
        });
    }

    @Test
    void testLevelUpUnitNotFound() {
        LevelUpRequest request = new LevelUpRequest();
        request.setParty(testParty);
        request.setUnitName("NonExistent");
        request.setHeroClass("WARRIOR");

        assertThrows(ResponseStatusException.class, () -> {service.levelUp(request);});
    }

    @Test
    void testLevelUpWithTransformation() {
        // Level ORDER to 5 for transformation
        for (int i = 0; i < 5; i++) {
            testUnit.addExperience(20000);
            testUnit.levelUpClass(HeroClass.ORDER);
        }

        LevelUpRequest request = new LevelUpRequest();
        request.setParty(testParty);
        request.setUnitName("Hero");
        request.setHeroClass("ORDER");

        testUnit.addExperience(20000);

        PartyResponse response = service.levelUp(request);

        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("transformed")));
        assertEquals(HeroClass.PRIEST, testUnit.getMainClass());
    }

    @Test
    void testUseItemSuccess() {
        testParty.addItem(Items.BREAD);
        testUnit.setHealth(50);

        UseItemRequest request = new UseItemRequest();
        request.setParty(testParty);
        request.setItemName("BREAD");
        request.setUnitName("Hero");

        int initialHealth = testUnit.getHealth();

        PartyResponse response = service.useItem(request);

        assertTrue(testUnit.getHealth() > initialHealth);
        assertEquals(0, testParty.getInventory().getOrDefault(Items.BREAD, 0));
        assertFalse(response.getMessages().isEmpty());
    }

    @Test
    void testUseItemUnknownItem() {
        UseItemRequest request = new UseItemRequest();
        request.setParty(testParty);
        request.setItemName("INVALID_ITEM");
        request.setUnitName("Hero");

        assertThrows(ResponseStatusException.class, () -> {
            service.useItem(request);
        });
    }

    @Test
    void testUseItemNotInInventory() {
        UseItemRequest request = new UseItemRequest();
        request.setParty(testParty);
        request.setItemName("ELIXIR");
        request.setUnitName("Hero");

        assertThrows(ResponseStatusException.class, () -> {
            service.useItem(request);
        });
    }

    @Test
    void testUseItemOnDeadUnitWithoutElixir() {
        testParty.addItem(Items.BREAD);
        testUnit.setHealth(0);

        UseItemRequest request = new UseItemRequest();
        request.setParty(testParty);
        request.setItemName("BREAD");
        request.setUnitName("Hero");

        assertThrows(ResponseStatusException.class, () -> {
            service.useItem(request);
        });
    }

    @Test
    void testUseElixirOnDeadUnit() {
        testParty.addItem(Items.ELIXIR);
        testUnit.setHealth(0);
        assertTrue(testUnit.isDead());

        UseItemRequest request = new UseItemRequest();
        request.setParty(testParty);
        request.setItemName("ELIXIR");
        request.setUnitName("Hero");

        PartyResponse response = service.useItem(request);

        assertTrue(testUnit.isAlive());
        assertEquals(testUnit.getMaxHealth(), testUnit.getHealth());
        assertEquals(testUnit.getMaxMana(), testUnit.getMana());
    }

    @Test
    void testGenerateRecruits() {
        List<Unit> recruits = service.generateRecruits();

        assertNotNull(recruits);
        assertFalse(recruits.isEmpty());
        assertTrue(recruits.size() >= 1 && recruits.size() <= 5);

        // All recruits should have base classes
        recruits.forEach(r -> assertTrue(r.getMainClass().isBase()));
    }

    @Test
    void testConfirmRecruitSuccess() {
        Unit recruit = new Unit("Recruit", HeroClass.MAGE);

        RecruitRequest request = new RecruitRequest();
        request.setParty(testParty);
        request.setRecruit(recruit);

        int initialGold = testParty.getGold();
        int initialSize = testParty.getUnits().size();

        PartyResponse response = service.confirmRecruit(request);

        assertEquals(initialSize + 1, testParty.getUnits().size());
        assertTrue(testParty.getUnits().contains(recruit));
        assertTrue(testParty.getGold() < initialGold || recruit.getLevel() == 1);
    }

    @Test
    void testConfirmRecruitPartyFull() {
        // Fill party to 5 units
        for (int i = 0; i < 4; i++) {
            testParty.addUnit(new Unit("Hero" + i, HeroClass.WARRIOR));
        }

        Unit recruit = new Unit("Recruit", HeroClass.MAGE);

        RecruitRequest request = new RecruitRequest();
        request.setParty(testParty);
        request.setRecruit(recruit);

        assertThrows(ResponseStatusException.class, () -> {
            service.confirmRecruit(request);
        });
    }

    @Test
    void testConfirmRecruitInsufficientGold() {
        testParty.setGold(10);

        // Create a high-level recruit
        Unit recruit = new Unit("Recruit", HeroClass.MAGE);
        recruit.addExperience(50000);
        recruit.levelUpClass(HeroClass.MAGE);
        recruit.levelUpClass(HeroClass.MAGE);

        RecruitRequest request = new RecruitRequest();
        request.setParty(testParty);
        request.setRecruit(recruit);

        assertThrows(ResponseStatusException.class, () -> {
            service.confirmRecruit(request);
        });
    }

    @Test
    void testConfirmRecruitLevel1Free() {
        testParty.setGold(0);

        Unit recruit = new Unit("Recruit", HeroClass.MAGE);
        assertEquals(1, recruit.getLevel());

        RecruitRequest request = new RecruitRequest();
        request.setParty(testParty);
        request.setRecruit(recruit);

        PartyResponse response = service.confirmRecruit(request);

        assertTrue(testParty.getUnits().contains(recruit));
        assertEquals(0, testParty.getGold());
    }

    @Test
    void testViewParty() {
        PartyResponse response = service.viewParty(testParty);

        assertNotNull(response);
        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("Test Party")));
        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("Hero")));
        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("Gold:")));
    }

    @Test
    void testViewPartyWithMultipleUnits() {
        testParty.addUnit(new Unit("Mage", HeroClass.MAGE));
        testParty.addUnit(new Unit("Warrior", HeroClass.WARRIOR));

        PartyResponse response = service.viewParty(testParty);

        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("Mage")));
        assertTrue(response.getMessages().stream()
                .anyMatch(m -> m.contains("Warrior")));
    }
}
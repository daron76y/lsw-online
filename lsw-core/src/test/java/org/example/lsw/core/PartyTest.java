package org.example.lsw.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PartyTest {
    private Party party;
    private Unit unit1;
    private Unit unit2;

    @BeforeEach
    void setUp() {
        party = new Party("Test Party");
        unit1 = new Unit("Hero1", HeroClass.WARRIOR);
        unit2 = new Unit("Hero2", HeroClass.MAGE);
    }

    @Test
    void testPartyCreation() {
        assertEquals("Test Party", party.getName());
        assertEquals(0, party.getGold());
        assertTrue(party.getUnits().isEmpty());
    }

    @Test
    void testAddUnit() {
        party.addUnit(unit1);

        assertEquals(1, party.getUnits().size());
        assertTrue(party.getUnits().contains(unit1));
    }

    @Test
    void testAddDuplicateUnit() {
        party.addUnit(unit1);

        assertThrows(IllegalStateException.class, () -> {
            party.addUnit(unit1);
        });
    }

    @Test
    void testPartyFull() {
        // Add 5 units
        for (int i = 0; i < 5; i++) {
            party.addUnit(new Unit("Hero" + i, HeroClass.WARRIOR));
        }

        assertThrows(IllegalStateException.class, () -> {
            party.addUnit(new Unit("Hero6", HeroClass.MAGE));
        });
    }

    @Test
    void testRemoveUnit() {
        party.addUnit(unit1);
        party.removeUnit(unit1);

        assertFalse(party.getUnits().contains(unit1));
    }

    @Test
    void testRemoveNonexistentUnit() {
        assertThrows(IllegalArgumentException.class, () -> {
            party.removeUnit(unit1);
        });
    }

    @Test
    void testGetAliveUnits() {
        party.addUnit(unit1);
        party.addUnit(unit2);

        unit1.setHealth(0); // Kill unit1

        assertEquals(1, party.getAliveUnits().size());
        assertTrue(party.getAliveUnits().contains(unit2));
    }

    @Test
    void testIsDefeated() {
        party.addUnit(unit1);
        party.addUnit(unit2);

        assertFalse(party.isDefeated());

        unit1.setHealth(0);
        unit2.setHealth(0);

        assertTrue(party.isDefeated());
    }

    @Test
    void testGetUnitByName() {
        party.addUnit(unit1);

        Unit found = party.getUnitByName("Hero1");

        assertNotNull(found);
        assertEquals(unit1, found);
    }

    @Test
    void testGetUnitByNameNotFound() {
        Unit found = party.getUnitByName("NonExistent");

        assertNull(found);
    }

    @Test
    void testGetUnitByNameDead() {
        party.addUnit(unit1);
        unit1.setHealth(0);

        Unit found = party.getUnitByName("Hero1");

        assertNull(found); // Dead units shouldn't be found
    }

    @Test
    void testGoldManagement() {
        party.setGold(500);
        assertEquals(500, party.getGold());

        party.setGold(party.getGold() - 200);
        assertEquals(300, party.getGold());
    }

    @Test
    void testInventory() {
        party.addItem(Items.BREAD);
        party.addItem(Items.BREAD);

        assertEquals(2, party.getInventory().get(Items.BREAD));

        party.removeItem(Items.BREAD);
        assertEquals(1, party.getInventory().get(Items.BREAD));

        // Can't go below 0
        party.removeItem(Items.BREAD);
        party.removeItem(Items.BREAD);
        assertEquals(0, party.getInventory().get(Items.BREAD));
    }

    @Test
    void testCumulativeLevels() {
        Unit hero1 = new Unit("Hero1", HeroClass.WARRIOR);
        Unit hero2 = new Unit("Hero2", HeroClass.MAGE);

        // Level them up
        hero1.addExperience(10000);
        hero1.levelUpClass(HeroClass.WARRIOR);
        hero2.addExperience(10000);
        hero2.levelUpClass(HeroClass.MAGE);

        party.addUnit(hero1);
        party.addUnit(hero2);

        assertTrue(party.getCumulativeLevels() >= 2);
    }

    @Test
    void testCanAnyUnitLevelAny() {
        party.addUnit(unit1);

        assertFalse(party.canAnyUnitLevelAny());

        unit1.addExperience(10000);
        assertTrue(party.canAnyUnitLevelAny());
    }
}
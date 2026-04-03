package org.example.lsw.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeroClassTest {

    @Test
    void testBaseClasses() {
        assertTrue(HeroClass.ORDER.isBase());
        assertTrue(HeroClass.CHAOS.isBase());
        assertTrue(HeroClass.WARRIOR.isBase());
        assertTrue(HeroClass.MAGE.isBase());
    }

    @Test
    void testSpecializations() {
        assertTrue(HeroClass.PRIEST.isSpecialization());
        assertTrue(HeroClass.INVOKER.isSpecialization());
        assertTrue(HeroClass.KNIGHT.isSpecialization());
        assertTrue(HeroClass.WIZARD.isSpecialization());
    }

    @Test
    void testHybrids() {
        assertTrue(HeroClass.HERETIC.isHybrid());
        assertTrue(HeroClass.PALADIN.isHybrid());
        assertTrue(HeroClass.ROGUE.isHybrid());
        assertTrue(HeroClass.WARLOCK.isHybrid());
    }

    @Test
    void testComboCreation() {
        assertEquals(HeroClass.PRIEST, HeroClass.comboOf(HeroClass.ORDER, HeroClass.ORDER));
        assertEquals(HeroClass.PALADIN, HeroClass.comboOf(HeroClass.ORDER, HeroClass.WARRIOR));
        assertEquals(HeroClass.ROGUE, HeroClass.comboOf(HeroClass.CHAOS, HeroClass.WARRIOR));
    }

    @Test
    void testComboSymmetry() {
        // Order shouldn't matter
        assertEquals(
                HeroClass.comboOf(HeroClass.ORDER, HeroClass.WARRIOR),
                HeroClass.comboOf(HeroClass.WARRIOR, HeroClass.ORDER)
        );
    }

    @Test
    void testStats() {
        // Warrior should have high attack/defense
        assertTrue(HeroClass.WARRIOR.getAttackPerLevel() > 0);
        assertTrue(HeroClass.WARRIOR.getDefensePerLevel() > 0);

        // Mage should have high mana
        assertTrue(HeroClass.MAGE.getManaPerLevel() > 0);
    }

    @Test
    void testAbilities() {
        assertFalse(HeroClass.WARRIOR.getAbilities().isEmpty());
        assertFalse(HeroClass.MAGE.getAbilities().isEmpty());
    }

    @Test
    void testHybridStats() {
        // Hybrid should combine parent stats
        int expectedAttack = HeroClass.ORDER.getAttackPerLevel() +
                HeroClass.WARRIOR.getAttackPerLevel();
        assertEquals(expectedAttack, HeroClass.PALADIN.getAttackPerLevel());
    }
}
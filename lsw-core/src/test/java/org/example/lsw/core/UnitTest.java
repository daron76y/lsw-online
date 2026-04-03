package org.example.lsw.core;

import org.example.lsw.core.abilities.Ability;
import org.example.lsw.core.effects.Shield;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnitTest {
    private Unit warrior;
    private Unit mage;

    @BeforeEach
    void setUp() {
        warrior = new Unit("TestWarrior", HeroClass.WARRIOR);
        mage = new Unit("TestMage", HeroClass.MAGE);
    }

    @Test
    void testUnitCreation() {
        assertEquals("TestWarrior", warrior.getName());
        assertEquals(HeroClass.WARRIOR, warrior.getMainClass());
        assertEquals(1, warrior.getLevel());
        assertTrue(warrior.isAlive());
    }

    @Test
    void testApplyDamage() {
        int initialHealth = warrior.getHealth();
        int damage = 20;

        int actualDamage = warrior.applyDamage(damage);

        // Damage should be reduced by defense
        assertTrue(actualDamage < damage);
        assertEquals(initialHealth - actualDamage, warrior.getHealth());
    }

    @Test
    void testDeath() {
        warrior.setHealth(0);
        assertTrue(warrior.isDead());
        assertFalse(warrior.isAlive());
    }

    @Test
    void testLevelUp() {
        warrior.addExperience(100000000);
        int initialLevel = warrior.getLevel();

        warrior.levelUpClass(HeroClass.WARRIOR);

        assertEquals(initialLevel + 1, warrior.getLevel());
        assertTrue(warrior.getAttack() > 5); // Should increase from base
    }

    @Test
    void testLevelUpInsufficientExperience() {
        assertThrows(IllegalArgumentException.class, () -> {
            warrior.levelUpClass(HeroClass.WARRIOR);
        });
    }

    @Test
    void testClassTransformation() {
        // Level ORDER to 5
        for (int i = 0; i < 5; i++) {
            warrior.addExperience(10000);
            warrior.levelUpClass(HeroClass.ORDER);
        }

        HeroClass transformed = warrior.handleClassTransformation();

        assertEquals(HeroClass.PRIEST, transformed);
        assertEquals(HeroClass.PRIEST, warrior.getMainClass());
    }

    @Test
    void testHybridTransformation() {
        // Level two different classes to 5
        for (int i = 0; i < 5; i++) {
            warrior.addExperience(20000);
            warrior.levelUpClass(HeroClass.ORDER);
        }

        // First transformation to specialist
        warrior.handleClassTransformation();
        assertEquals(HeroClass.PRIEST, warrior.getMainClass());

        // Level CHAOS to 5
        for (int i = 0; i < 5; i++) {
            warrior.addExperience(20000);
            warrior.levelUpClass(HeroClass.CHAOS);
        }

        HeroClass hybrid = warrior.handleClassTransformation();

        assertTrue(hybrid.isHybrid());
    }

    @Test
    void testEffects() {
        Shield shield = new Shield(50);
        warrior.addEffect(shield);

        assertTrue(warrior.getEffects().contains(shield));

        warrior.removeEffect(shield);
        assertFalse(warrior.getEffects().contains(shield));
    }

    @Test
    void testAbilityRetrieval() {
        Ability ability = mage.getAbilityByName("Replenish");

        assertNotNull(ability);
        assertEquals("Replenish", ability.getName());
    }

    @Test
    void testMaxLevel() {
        // Try to level beyond 20
        for (int i = 0; i < 30; i++) {
            warrior.addExperience(50000);
            try {
                warrior.levelUpClass(HeroClass.WARRIOR);
            } catch (IllegalArgumentException e) {
                // Expected when reaching max level
            }
        }

        assertTrue(warrior.getLevel() <= 20);
    }

    @Test
    void testManaManagement() {
        int initialMana = mage.getMana();

        mage.setMana(mage.getMana() - 30);
        assertEquals(initialMana - 30, mage.getMana());

        // Can't exceed max mana
        mage.setMana(mage.getMaxMana() + 100);
        assertEquals(mage.getMaxMana(), mage.getMana());

        // Can't go below 0
        mage.setMana(-50);
        assertEquals(0, mage.getMana());
    }
}
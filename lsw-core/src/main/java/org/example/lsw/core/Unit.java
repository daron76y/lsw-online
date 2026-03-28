package org.example.lsw.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.example.lsw.core.abilities.Ability;
import org.example.lsw.core.effects.Effect;

import java.util.*;

@JsonAutoDetect(
    fieldVisibility    = JsonAutoDetect.Visibility.ANY,
    getterVisibility   = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonPropertyOrder({ //ensures health and mana get set before the maxes to avoid any strange int default to 0 errors. TODO: May be redundant.
        "name", "mainClass", "classLevels", "experience",
        "attack", "defense",
        "maxHealth", "maxMana", "health", "mana",
        "abilities", "effects"
})
public class Unit {
    private final String name;

    private final EnumMap<HeroClass, Integer> classLevels;
    private HeroClass mainClass;

    private final List<Effect> effects;
    private final List<Ability> abilities;
    private int experience;

    private int maxHealth;
    private int maxMana;
    private int attack;
    private int defense;
    private int health;
    private int mana;

    /** No-arg constructor for Jackson deserialization. */
    private Unit() {
        this.name = null;
        this.classLevels = new EnumMap<>(HeroClass.class);
        this.effects = new ArrayList<>();
        this.abilities = new ArrayList<>();
    }

    //Constructor
    public Unit(String name, int atk, int def, int maxHp, int maxMp, HeroClass startingClass) {
        this.name = name;

        classLevels = new EnumMap<>(HeroClass.class);
        classLevels.put(HeroClass.ORDER, 0);
        classLevels.put(HeroClass.CHAOS, 0);
        classLevels.put(HeroClass.WARRIOR, 0);
        classLevels.put(HeroClass.MAGE, 0);
        mainClass = startingClass;
        classLevels.put(mainClass, 1); //init starting class to level 1

        effects = new ArrayList<>(startingClass.getEffects());
        abilities = new ArrayList<>(startingClass.getAbilities());
        experience = 0;

        this.attack = atk;
        this.defense = def;
        this.health = this.maxHealth = maxHp;
        this.mana = this.maxMana = maxMp;
    }

    public Unit(String name, HeroClass startingClass) {
        //default stats constructor
        this(name, 5, 5, 100, 50, startingClass);
    }

    // -----------------------------------------------------------------------
    // |                                Basic                                |
    // -----------------------------------------------------------------------
    public String getName() {return name;}

    public int getMaxHealth() {return maxHealth;}

    public void setMaxHealth(int maxHealth) {this.maxHealth = maxHealth;}

    public int getMaxMana() {return maxMana;}

    public void setMaxMana(int maxMana) {this.maxMana = maxMana;}

    public int getAttack() {return attack;}

    public void setAttack(int attack) {this.attack = attack;}

    public int getDefense() {return defense;}

    public void setDefense(int defence) {this.defense = defence;}

    public int getHealth() {return health;}

    public void setHealth(int health) {this.health = Math.min(Math.max(0, health), maxHealth);}

    public int getMana() {return mana;}

    public void setMana(int mana) {this.mana = Math.min(Math.max(0, mana), maxMana);}


    // -----------------------------------------------------------------------
    // |                              Effects                                |
    // -----------------------------------------------------------------------
    public List<Effect> getEffects() {return this.effects;}
    public void addEffect(Effect effect) {this.effects.add(effect);}
    public void removeEffect(Effect effect) {this.effects.remove(effect);}

    public void clearDebuffEffects() {
        for (Effect effect : effects) {
            if (!mainClass.getEffects().contains(effect)) {
                removeEffect(effect);
            }
        }
    }

    // -----------------------------------------------------------------------
    // |                             Abilities                               |
    // -----------------------------------------------------------------------
    public List<Ability> getAbilities() {return this.abilities;}
    public void addAbility(Ability ability) {this.abilities.add(ability);}
    public void removeAbility(Ability ability) {this.abilities.remove(ability);}

    public Ability getAbilityByName(String name) {
        return abilities.stream()
                .filter(ability -> ability.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    // -----------------------------------------------------------------------
    // |                               Battle                                |
    // -----------------------------------------------------------------------
    public boolean isAlive() {return health > 0;}
    public boolean isDead() {return health <= 0;}

    public int applyDamage(int damage) {
        //apply defense
        damage -= defense;

        //ensure damage is not negative
        damage = Math.max(0, damage);

        //inflict damage onto health
        health = Math.max(0, health - damage);

        //return damage dealt
        return damage;
    }

    // -----------------------------------------------------------------------
    // |                               Classes                               |
    // -----------------------------------------------------------------------
    public HeroClass getMainClass() {return mainClass;}
    public EnumMap<HeroClass, Integer> getClassLevels() {return classLevels;}

    public void changeMainClass(HeroClass heroClass) {
        if (mainClass == heroClass) throw new IllegalArgumentException("This unit is already a " + heroClass);
        if (mainClass.isHybrid()) throw new IllegalArgumentException("This unit is a permanent hybrid!");
        if (mainClass.isSpecialization() && !heroClass.isHybrid()) throw new IllegalArgumentException("Specialized units may only upgrade to hybrids!");
        if (heroClass.isSpecialization() && classLevels.get(heroClass.getParentA()) < 5) throw new IllegalArgumentException("This unit does not meet the minimum level to specialize into " + heroClass);
        if (heroClass.isHybrid() && (classLevels.get(heroClass.getParentA()) < 5 || classLevels.get(heroClass.getParentB()) < 5)) throw new  IllegalArgumentException("This unit does not meet he minimum levels to hybridize into " + heroClass);

        //unit may change class
        mainClass = heroClass;
        abilities.clear();
        abilities.addAll(heroClass.getAbilities());
        effects.clear();
        effects.addAll(heroClass.getEffects());
    }

    public void levelUpClass(HeroClass heroClass) {
        if (!classLevels.containsKey(heroClass)) throw new IllegalArgumentException("This unit does not have this class");
        if (getLevel() >= 20) throw new IllegalArgumentException("This unit is at the max level: " + getLevel());
        if (experience < expNeededForLvl(classLevels.get(heroClass) + 1)) throw new IllegalArgumentException("Not enough experience!");

        //level up class and get stat bonuses
        experience -= expNeededForLvl(classLevels.get(heroClass) + 1);
        classLevels.put(heroClass, classLevels.get(heroClass) + 1);
        attack += 1 + heroClass.getAttackPerLevel();
        defense += 1 + heroClass.getDefensePerLevel();
        health = maxHealth += 5 + heroClass.getHealthPerLevel();
        mana = maxMana += 2 + heroClass.getManaPerLevel();
    }

    public HeroClass handleClassTransformation() {
        List<HeroClass> lvl5Classes = classLevels.entrySet().stream()
                .filter(entry -> entry.getValue() >= 5)
                .map(Map.Entry::getKey)
                .toList();

        //transform into specialization if one class is lvl 5 and you're not already a specialization
        if (mainClass.isBase() && lvl5Classes.size() == 1) {
            HeroClass hc = lvl5Classes.getFirst();
            changeMainClass(HeroClass.comboOf(hc, hc));
            return mainClass;
        }

        //if two classes are lvl 5, transform into a hybrid of the two as long as you are currently a specialization
        else if (mainClass.isSpecialization() && lvl5Classes.size() == 2) {
            HeroClass parentA = lvl5Classes.get(0);
            HeroClass parentB = lvl5Classes.get(1);
            changeMainClass(HeroClass.comboOf(parentA, parentB));
            return mainClass;
        }

        //if above two don't work, then do nothing
        return null;
    }

    public List<HeroClass> getClassesAvailableForLevelUp() {
        List<HeroClass> classes = new ArrayList<>();
        for (HeroClass heroClass : classLevels.keySet())
            if (canLevelUpClass(heroClass)) classes.add(heroClass);
        return classes;
    }



    // -----------------------------------------------------------------------
    // |                           Levels & Exp                              |
    // -----------------------------------------------------------------------
    public int getLevel() {
        return classLevels.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public boolean canLevelUpClass(HeroClass heroClass) {
        if (!classLevels.containsKey(heroClass)) return false;
        if (getLevel() >= 20) return false;
        return experience >= expNeededForLvl(classLevels.get(heroClass) + 1);
    }

    public boolean canLevelUpAny() {
        for (HeroClass hc : classLevels.keySet()) {
            if (canLevelUpClass(hc)) return true;
        }
        return false;
    }

    public int getExperience() {return experience;}
    public void addExperience(int experience) {this.experience += experience;}
    public void loseExperience(int experience) {this.experience -= Math.min(experience, this.experience);}

    public int expNeededForLvl(int lvl) {
        if (lvl <= 0) return 0;
        return expNeededForLvl(lvl - 1) + 500 + 75 * lvl + 20 * lvl * lvl;
    }


    // -----------------------------------------------------------------------
    // |                               Other                                 |
    // -----------------------------------------------------------------------
    @Override
    public String toString() {
        return String.format("[%s]\tatk: %d|def: %d|hp: %d|mp: %d|lvl: %d|xp: %d", name, attack,  defense, health, mana, getLevel(), experience);
    }
}

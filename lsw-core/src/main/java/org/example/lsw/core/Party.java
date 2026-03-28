package org.example.lsw.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

@JsonAutoDetect(
        fieldVisibility    = JsonAutoDetect.Visibility.ANY,
        getterVisibility   = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public class Party {
    private final String name;
    private int gold;
    private final EnumMap<Items, Integer> inventory;
    private final List<Unit> units;

    /** No-arg constructor for Jackson deserialization. */
    private Party() {
        this.name = null;
        this.units = new ArrayList<>();
        this.gold = 0;
        this.inventory = new EnumMap<>(Items.class);
    }

    //Constructors
    public Party(String name) {
        this.name = name;
        this.units = new ArrayList<Unit>();
        this.gold = 0;
        this.inventory = new EnumMap<>(Items.class);
    }

    //Methods

    // -----------------------------------------------------------------------
    // |                         Party-related info                          |
    // -----------------------------------------------------------------------
    public String getName() {return name;}

    public int getCumulativeLevels() {
        return units.stream().mapToInt(Unit::getLevel).sum();
    }

    public boolean isDefeated() {
        return units.stream().allMatch(Unit::isDead);
    }

    public int getGold() {return gold;}

    public void setGold(int gold) {this.gold = gold;}

    public EnumMap<Items, Integer> getInventory() {return inventory;}

    public void addItem(Items item) {inventory.put(item, inventory.getOrDefault(item, 0) + 1);}

    public void removeItem(Items item) {inventory.put(item, Math.max(inventory.getOrDefault(item, 0) - 1, 0));}

    // -----------------------------------------------------------------------
    // |                                Units                                |
    // -----------------------------------------------------------------------
    public void addUnit(Unit unit) {
        if (units.contains(unit)) throw new IllegalStateException("Party already contains unit: " + unit.getName());
        if (units.size() >= 5) throw new IllegalStateException("Party is full");
        units.add(unit);
    }

    public void removeUnit(Unit unit) {
        if (units.contains(unit)) units.remove(unit);
        else throw new IllegalArgumentException("Party does not contain unit: " + unit.getName());
    }

    public List<Unit> getUnits() {return units;}

    public List<Unit> getAliveUnits() {
        return units.stream().filter(Unit::isAlive).toList();
    }

    public int getNumAliveUnits() {
        return getAliveUnits().size();
    }

    public Unit getUnitByName(String name) {
        return units.stream()
                .filter(unit -> unit.getName().equalsIgnoreCase(name) && unit.isAlive())
                .findFirst()
                .orElse(null);
    }

    // -----------------------------------------------------------------------
    // |                                Other                                |
    // -----------------------------------------------------------------------
    public boolean canAnyUnitLevelAny() {
        return getAliveUnits().stream().anyMatch(Unit::canLevelUpAny);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Party Name: ").append(name).append("\n");
        units.forEach(unit -> {
            sb.append(unit.toString()).append("\n");
        });
        return sb.toString();
    }
}

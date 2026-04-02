package org.example.lsw.party;

import org.example.lsw.core.*;
import org.example.lsw.core.abilities.Ability;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * All operations in this service receive a party in the request body,
 * modify it, and then returns. The caller is responsible for saving the
 * result back to user-service. This keeps party-service stateless.
 */
@Service
public class PartyService {
    private final UnitFactory unitFactory = new UnitFactoryCSV();

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                         level up                         │
    //      └──────────────────────────────────────────────────────────┘
    public PartyResponse levelUp(LevelUpRequest req) {
        List<String> messages = new ArrayList<>();

        //get the unit to level up
        Unit unit = findUnit(req.getParty(), req.getUnitName());

        //get the hero class to level up
        HeroClass heroClass;
        try {
            heroClass = HeroClass.valueOf(req.getHeroClass().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown class: " + req.getHeroClass());
        }

        //try to level up the unit
        try {
            unit.levelUpClass(heroClass);
            messages.add(unit.getName() + " leveled up " + heroClass + " to Lv." + unit.getClassLevels().get(heroClass) + "!");
            messages.add(unit.toString());

            //if the unit transformed into a new class, then show a special message for that
            HeroClass transformed = unit.handleClassTransformation();
            if (transformed != null) messages.add(unit.getName() + " transformed into " + transformed + "!");

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        return new PartyResponse(req.getParty(), messages);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                         use item                         │
    //      └──────────────────────────────────────────────────────────┘
    public PartyResponse useItem(UseItemRequest req) {
        List<String> messages = new ArrayList<>();
        Party party = req.getParty();

        //get the item
        Items item;
        try {
            item = Items.valueOf(req.getItemName().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown item: " + req.getItemName());
        }

        //ensure the party actually has the item
        if (!party.getInventory().containsKey(item) || party.getInventory().get(item) <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party does not have a " + item + ".");

        //get and validate the unit is alive, unless we're using an elixir
        Unit unit = findUnit(party, req.getUnitName());
        if (unit.isDead() && item != Items.ELIXIR)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, unit.getName() + " is dead. Use an ELIXIR to revive first.");

        //use the item
        party.removeItem(item);
        unit.setHealth(Math.min(Math.max(unit.getHealth() + item.getHealthBoost(), unit.getMaxHealth()), unit.getMaxHealth()));
        unit.setMana(Math.min(Math.max(unit.getMana() + item.getManaBoost(), unit.getMaxMana()), unit.getMaxMana()));

        messages.add(unit.getName() + " used " + item + ".");
        messages.add(unit.toString());

        return new PartyResponse(party, messages);
    }

    // ═══════════════════════════════════ Recruits ════════════════════════════════════

    /**
     * Generates a fresh set of hero recruit candidates (1–5 units).
     * The client shows these to the player, who picks one and calls confirmRecruit().
     */
    public List<Unit> generateRecruits() {
        int count = new Random().nextInt(5) + 1; //1-5
        return unitFactory.generateHeroRecruits(count);
    }

    /**
     * Adds the chosen recruit to the party (if affordable and party not full).
     */
    public PartyResponse confirmRecruit(RecruitRequest req) {
        List<String> messages = new ArrayList<>();
        Party party = req.getParty();
        Unit recruit = req.getRecruit();

        //disallow hiring recruits if party is full
        if (party.getUnits().size() >= 5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party is full (5/5).");

        //get cost of the recruit, calculated via its level
        int cost = recruit.getLevel() == 1 ? 0 : recruit.getLevel() * 200;

        //ensure party has enough gold to hire this recruit
        if (party.getGold() < cost)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough gold. Need " + cost + "g, have " + party.getGold() + "g.");

        //purchase and hire this recruit
        party.setGold(party.getGold() - cost);
        party.addUnit(recruit);
        messages.add("Recruited " + recruit.getName() + " [" + recruit.getMainClass() + " Lv." + recruit.getLevel() + "] for " + cost + "g.");

        return new PartyResponse(party, messages);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                        view party                        │
    //      └──────────────────────────────────────────────────────────┘
    public PartyResponse viewParty(Party party) {
        List<String> messages = new ArrayList<>();
        messages.add("=== " + party.getName() + " ===");
        for (Unit u : party.getUnits())
            messages.add(u.toString() + " | " + u.getMainClass() + " | abilities: " + u.getAbilities().stream()
                    .map(Ability::getName).toList());
        messages.add("Gold: " + party.getGold());
        messages.add("Inventory: " + party.getInventory());
        return new PartyResponse(party, messages);
    }

    //helper methods:
    private Unit findUnit(Party party, String unitName) {
        return party.getUnits().stream()
                .filter(u -> u.getName().equalsIgnoreCase(unitName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No unit named: " + unitName));
    }
}

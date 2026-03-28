package org.example.lsw.campaign;

import org.example.lsw.core.*;
import org.example.lsw.core.battle.BattleState;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Largest class in the project. Contains all logic for the PVE Campaign portion of our game.
 * All logic for overworld, Inn, redirecting to battles, rewards, penalities, calculating final score
 */
@Service
public class CampaignService {
    private final CampaignSessionRepository repo; //repo for storing campaign session info
    private final BattleServiceClient battleClient; //for redirecting battle calls to the battle service
    private final UnitFactory unitFactory = new UnitFactoryCSV(); //for enemy and recruit generation

    public CampaignService(CampaignSessionRepository repo, BattleServiceClient battleClient) {
        this.repo = repo;
        this.battleClient = battleClient;
    }

    //      ╔══════════════════════════════════════════════════════════╗
    //      ║                      Start Campaign                      ║
    //      ╚══════════════════════════════════════════════════════════╝
    public CampaignStateResponse startCampaign(StartCampaignRequest req) {
        //start a new campaign using a new session UUID
        String sessionId = UUID.randomUUID().toString();
        CampaignState state = new CampaignState(sessionId, req.getUsername(), req.getParty());

        //resume from a saved room if specified
        if (req.getStartRoom() > 0) { //if the starting room is not 0, it must not be a new campaign. Thus, resume.
            state.setCurrentRoom(req.getStartRoom());
            state.addMessage("Campaign resumed at room " + req.getStartRoom() + ". Welcome back, " + req.getParty().getName() + "!");
        } else { //new campaign
            state.addMessage("Campaign started! Welcome, " + req.getParty().getName() + "!");
        }
        state.addMessage("Actions: [next] [use-item] [level-up] [view-party] [quit]"); //initial prompt

        //save the campaign session to the repo and return a response of the state
        repo.save(new CampaignSessionEntity(state));
        return CampaignStateResponse.from(state);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                        get state                         │
    //      └──────────────────────────────────────────────────────────┘
    public CampaignStateResponse getState(String sessionId) {
        CampaignState state = loadState(sessionId);
        state.clearMessages();
        addPhasePrompt(state);
        return CampaignStateResponse.from(state);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                    process an action                     │
    //      └──────────────────────────────────────────────────────────┘
    public CampaignStateResponse processAction(String sessionId, CampaignActionRequest req) {
        //get the campaign state from its UUID
        CampaignState state = loadState(sessionId);
        state.clearMessages();

        //disallow sending actions to a finished campaign
        if (state.isFinished()) {
            state.addMessage("Campaign is already finished.");
            return CampaignStateResponse.from(state);
        }

        //handle the action depending on which phase it applies to
        String action = req.getAction().toLowerCase().trim();
        switch (state.getPhase()) {
            case OVERWORLD -> handleOverworldAction(state, action, req);
            case INN       -> handleInnAction(state, action, req);
            case BATTLE    -> handleBattleResolution(state);
            default        -> state.addMessage("Unknown phase.");
        }

        //after applying the action to the state, save the modified state to the repo and return a response
        saveState(sessionId, state);
        return CampaignStateResponse.from(state);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                    overworld actions                     │
    //      └──────────────────────────────────────────────────────────┘
    private void handleOverworldAction(CampaignState state, String action, CampaignActionRequest req) {
        switch (action) {
            case "next"       -> nextRoom(state);
            case "quit"       -> quit(state);
            case "use-item"   -> useItem(state, req.getItemName(), req.getUnitName());
            case "level-up"   -> levelUp(state, req.getUnitName(), req.getHeroClass());
            case "view-party" -> viewParty(state);
            default           -> state.addMessage("Unknown action: " + action + ". Valid: next | use-item | level-up | view-party | quit");
        }
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                   enter the next room                    │
    //      └──────────────────────────────────────────────────────────┘
    private void nextRoom(CampaignState state) {
        //setup room number info
        state.setCurrentRoom(state.getCurrentRoom() + 1);
        int room = state.getCurrentRoom();
        state.addMessage("Entering room " + room + "...");

        //finish the campaign if this is the last room
        if (room > state.getTotalRooms()) {
            finishCampaign(state);
            return;
        }

        //determine room type if not yet discovered, based on player cumulative levels
        if (state.getRoomTypes()[room] == null) {
            int shift = state.getPlayerParty().getCumulativeLevels() / 10 * 3;
            state.setRoomType(room, Math.random() * 100 <= 60 + shift ? CampaignState.RoomType.BATTLE : CampaignState.RoomType.INN
            );
        }

        //depending on the room type, enter a battle or enter an inn
        if (state.getRoomTypes()[room] == CampaignState.RoomType.BATTLE) enterBattle(state);
        else enterInn(state);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      enter a battle                      │
    //      └──────────────────────────────────────────────────────────┘
    private void enterBattle(CampaignState state) {
        state.addMessage("A battle begins!");
        Party enemyParty = unitFactory.generateEnemyParty(state.getPlayerParty().getCumulativeLevels());

        //delegate battle to battle-service via the battleClient
        String battleId = battleClient.startBattle(state.getSessionId(), state.getPlayerParty(), enemyParty);

        //ensure this campaign recognizes its currently in that battle
        state.setActiveBattleSessionId(battleId);
        state.setPhase(CampaignState.Phase.BATTLE);

        //TODO: remove these messages. Remmnants from deliv. 1!!!!
        state.addMessage("Battle session started. Use the battle API with battleId: " + battleId);
        state.addMessage("When the battle is finished, call POST /campaign/{id}/action with action=resolve-battle");
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                       enter an Inn                       │
    //      └──────────────────────────────────────────────────────────┘
    private void enterInn(CampaignState state) {
        //set the last encountered inn here (for when the player dies) and set the campaign phase
        state.setLastInnCheckpoint(state.getCurrentRoom());
        state.setPhase(CampaignState.Phase.INN);

        //restore all heroes
        //TODO: revive too? check requirements
        for (Unit u : state.getPlayerParty().getUnits()) {
            u.setHealth(u.getMaxHealth());
            u.setMana(u.getMaxMana());
        }

        //messages
        state.addMessage("You found an inn! All heroes restored.");
        state.addMessage("Inn actions: [buy <item>] [recruit] [confirm-recruit <index>] [leave]");
        state.addMessage("Items: " + Arrays.toString(Items.values()));
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                    resolving a battle                    │
    //      └──────────────────────────────────────────────────────────┘
    private void handleBattleResolution(CampaignState state) {
        //get battle id
        String battleId = state.getActiveBattleSessionId();
        if (battleId == null) {
            state.addMessage("No active battle to resolve.");
            return;
        }

        //get the result of a battle and disassociate it from this campaign by setting the active session to null
        BattleState result = battleClient.getBattleResult(battleId);
        battleClient.deleteBattle(battleId);
        state.setActiveBattleSessionId(null);

        //determine if player won (playerParty is always partyA)
        boolean playerWon = result.getWinnerPartyName().equals(state.getPlayerParty().getName());

        // update player party with post-battle state from battle-service
        // (battle-service revived both parties already)
        // TODO make it not revive player party in pve
        Party updatedPlayerParty = result.getPartyA();
        // If the party names match, use it; battle-service preserves party A = player party, but this is just for safety.
        if (!updatedPlayerParty.getName().equals(state.getPlayerParty().getName())) updatedPlayerParty = result.getPartyB();

        //redirect for rewards or penalties
        if (playerWon) { //reward if won
            state.addMessage("Victory!");
            awardBattleRewards(state, result.getPartyB());
        } else { //apply penalties and return to last Inn on death
            state.addMessage("Defeat! Returning to last inn checkpoint...");
            applyDefeatPenalties(state);
            state.setCurrentRoom(state.getLastInnCheckpoint());
            enterInn(state);
            return;
        }

        //return to the overworld phase of the campaign, and display its prompt
        state.setPhase(CampaignState.Phase.OVERWORLD);
        addPhasePrompt(state);
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      battle rewards                      │
    //      └──────────────────────────────────────────────────────────┘
    //TODO ensure lines up with requirements calculations
    private void awardBattleRewards(CampaignState state, Party defeatedEnemyParty) {
        //total exp and gold calculated from number of defeated enemies
        int totalExp  = 0;
        int totalGold = 0;
        for (Unit enemy : defeatedEnemyParty.getUnits()) {
            totalExp  += 50 * enemy.getLevel();
            totalGold += 75 * enemy.getLevel();
        }

        //divide up exp amongst all alive units
        List<Unit> alive = state.getPlayerParty().getAliveUnits();
        int share = alive.isEmpty() ? 0 : totalExp / alive.size();
        alive.forEach(u -> u.addExperience(share));

        //add gold to the player party
        state.getPlayerParty().setGold(state.getPlayerParty().getGold() + totalGold);

        //message
        state.addMessage("+" + totalGold + " gold | +" + totalExp + " total XP");
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                     defeat penalties                     │
    //      └──────────────────────────────────────────────────────────┘
    private void applyDefeatPenalties(CampaignState state) {
        Party party = state.getPlayerParty();
        party.setGold(Math.max(0, party.getGold() - (int)(party.getGold() * 0.10)));                 //lose 10% gold
        party.getAliveUnits().forEach(u -> u.loseExperience((int)(u.getExperience() * 0.30))); //lose 30% unit exp
        state.addMessage("-10% gold | -30% XP per hero");
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                   handling inn actions                   │
    //      └──────────────────────────────────────────────────────────┘
    private void handleInnAction(CampaignState state, String action, CampaignActionRequest req) {
        switch (action) {
            case "buy"            -> buyItem(state, req.getItemName());
            case "recruit"        -> showRecruits(state);
            case "confirm-recruit"-> confirmRecruit(state, req.getRecruitIndex());
            case "view-party"     -> viewParty(state);
            case "leave"          -> {
                state.setPhase(CampaignState.Phase.OVERWORLD);
                state.addMessage("Leaving inn. Actions: [next] [use-item] [level-up] [view-party] [quit]");
            }
            default -> state.addMessage("Unknown inn action: " + action + ". Valid: buy <item> | recruit | confirm-recruit <index> | leave");
        }
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      buying an item                      │
    //      └──────────────────────────────────────────────────────────┘
    private void buyItem(CampaignState state, String itemName) {
        if (itemName == null) { state.addMessage("Specify an item name."); return; }
        try {
            Items item = Items.valueOf(itemName.toUpperCase());
            Party party = state.getPlayerParty();
            if (party.getGold() < item.getCost()) {
                state.addMessage("Not enough gold. Need " + item.getCost() + "g, have " + party.getGold() + "g.");
                return;
            }
            party.setGold(party.getGold() - item.getCost());
            party.addItem(item);
            state.addMessage("Purchased " + item + " for " + item.getCost() + "g.");
        } catch (IllegalArgumentException e) {
            state.addMessage("Unknown item: " + itemName + ". Valid: " + Arrays.toString(Items.values()));
        }
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                 show available recruits                  │
    //      └──────────────────────────────────────────────────────────┘
    private void showRecruits(CampaignState state) {
        //full-parties cannot recruit new heroes
        if (state.getPlayerParty().getUnits().size() >= 5) {
            state.addMessage("Party is full (5/5).");
            return;
        }

        //generate and STORE recruits in state so confirm-recruit can access them in the next http call
        List<Unit> recruits = unitFactory.generateHeroRecruits(new Random().nextInt(3) + 1);
        state.setAvailableRecruits(recruits);

        //display recruits in a list
        state.addMessage("Available recruits:");
        for (int i = 0; i < recruits.size(); i++) {
            Unit r = recruits.get(i);
            int cost = r.getLevel() == 1 ? 0 : r.getLevel() * 200;
            state.addMessage(i + ": " + r.getName() + " [" + r.getMainClass() +
                    " Lv." + r.getLevel() + "] Cost: " + cost + "g");
        }
        state.addMessage("Type: confirm-recruit <index> to hire.");
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                 confirm (hire) a recruit                 │
    //      └──────────────────────────────────────────────────────────┘
    private void confirmRecruit(CampaignState state, Integer index) {
        List<Unit> recruits = state.getAvailableRecruits();

        //ensure recruit list isn't empty
        if (recruits == null || recruits.isEmpty()) {
            state.addMessage("No recruits available. Type 'recruit' first to see the list.");
            return;
        }

        //ensure the index chosen is actually valid
        if (index == null || index < 0 || index >= recruits.size()) {
            state.addMessage("Invalid index. Choose 0 to " + (recruits.size() - 1) + ".");
            return;
        }

        //get recruit, cost, and party
        Unit recruit = recruits.get(index);
        int cost = recruit.getLevel() == 1 ? 0 : recruit.getLevel() * 200;
        Party party = state.getPlayerParty();

        //ensure you can't hire with a full party. May be redundant idk.
        if (party.getUnits().size() >= 5) {
            state.addMessage("Party is full (5/5). Cannot recruit.");
            return;
        }

        //ensure player has enough gold to hire
        if (party.getGold() < cost) {
            state.addMessage("Not enough gold. Need " + cost + "g, have " + party.getGold() + "g.");
            return;
        }

        //perform the recruitment
        party.setGold(party.getGold() - cost);
        party.addUnit(recruit);
        state.setAvailableRecruits(new java.util.ArrayList<>());
        state.addMessage(recruit.getName() + " has joined your party!");
        state.addMessage("Party gold: " + party.getGold() + "g");
        state.addMessage("Inn actions: [buy <item>] [recruit] [confirm-recruit <index>] [leave]");
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                         use item                         │
    //      └──────────────────────────────────────────────────────────┘
    private void useItem(CampaignState state, String itemName, String unitName) {
        if (itemName == null || unitName == null) {
            state.addMessage("Usage: use-item, itemName=<item>, unitName=<unit>");
            return;
        }
        try {
            Items item = Items.valueOf(itemName.toUpperCase());
            Party party = state.getPlayerParty();
            if (!party.getInventory().containsKey(item) || party.getInventory().get(item) <= 0) {
                state.addMessage("You don't have a " + item + ".");
                return;
            }
            Unit unit = party.getUnits().stream()
                    .filter(u -> u.getName().equalsIgnoreCase(unitName))
                    .findFirst().orElse(null);
            if (unit == null) { state.addMessage("No such unit: " + unitName); return; }
            if (unit.isDead() && item != Items.ELIXIR) {
                state.addMessage(unitName + " must be revived with an ELIXIR first.");
                return;
            }
            party.removeItem(item);
            unit.setHealth(Math.min(unit.getHealth() + item.getHealthBoost(), unit.getMaxHealth()));
            unit.setMana(Math.min(unit.getMana() + item.getManaBoost(), unit.getMaxMana()));
            state.addMessage(unitName + " used " + item + ".");
        } catch (IllegalArgumentException e) {
            state.addMessage("Unknown item: " + itemName);
        }
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                         level up                         │
    //      └──────────────────────────────────────────────────────────┘
    private void levelUp(CampaignState state, String unitName, String heroClassName) {
        //if no arguments given, show which units and classes are available to level up
        if (unitName == null || heroClassName == null) {
            showLevelUpMenu(state);
            return;
        }

        //get unit
        Unit unit = state.getPlayerParty().getUnits().stream()
                .filter(u -> u.getName().equalsIgnoreCase(unitName))
                .findFirst().orElse(null);
        if (unit == null) { state.addMessage("No such unit: " + unitName); return; }

        //level up the chosen class
        try {
            HeroClass hc = HeroClass.valueOf(heroClassName.toUpperCase());
            unit.levelUpClass(hc);
            state.addMessage(unitName + " leveled up " + hc + " to Lv." + unit.getClassLevels().get(hc) + " (XP remaining: " + unit.getExperience() + ")");
            HeroClass transformed = unit.handleClassTransformation();
            if (transformed != null)
                state.addMessage(unitName + " transformed into " + transformed + "!");
            showLevelUpMenu(state); //how updated level-up options after leveling
        } catch (IllegalArgumentException e) {
            state.addMessage("Error: " + e.getMessage());
        }
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                    show level up menu                    │
    //      └──────────────────────────────────────────────────────────┘
    private void showLevelUpMenu(CampaignState state) {
        //if no units can level up
        if (!state.getPlayerParty().canAnyUnitLevelAny()) {
            state.addMessage("No units have enough XP to level up.");
            state.addMessage("Tip: type 'view-party' to see each unit's XP.");
            return;
        }

        //show level-able units
        state.addMessage("=== Level Up ===");
        for (Unit u : state.getPlayerParty().getAliveUnits()) {
            if (u.canLevelUpAny()) {
                List<String> options = u.getClassesAvailableForLevelUp().stream()
                        .map(hc -> hc + "(Lv." + u.getClassLevels().get(hc) + "→" + (u.getClassLevels().get(hc) + 1) + ")")
                        .toList();
                state.addMessage("  " + u.getName() + " [XP:" + u.getExperience() + "] can level up: " + options);
            }
        }
        state.addMessage("Usage: level-up <unit> <class>");
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │           view the party, gold, and inventory            │
    //      └──────────────────────────────────────────────────────────┘
    private void viewParty(CampaignState state) {
        state.addMessage("=== Party: " + state.getPlayerParty().getName() + " ===");
        for (Unit u : state.getPlayerParty().getUnits())
            state.addMessage(u.toString() + " | class: " + u.getMainClass());
        state.addMessage("Gold: " + state.getPlayerParty().getGold());
        state.addMessage("Inventory: " + state.getPlayerParty().getInventory());
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      quit campaign                       │
    //      └──────────────────────────────────────────────────────────┘
    //TODO: check if this is even still needed
    private void quit(CampaignState state) {
        state.addMessage("Campaign saved at room " + state.getCurrentRoom() + ". Bye-Bye!");
        //the service layer will save the state. Client should save campaign progress
        //to user-service via POST: /api/users/{username}/campaign
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                    campaign finished                     │
    //      └──────────────────────────────────────────────────────────┘
    private void finishCampaign(CampaignState state) {
        state.setFinished(true);
        state.setPhase(CampaignState.Phase.FINISHED);
        state.addMessage("Campaign complete! You conquered all " + state.getTotalRooms() + " rooms!");

        //calculate final score
        Party party = state.getPlayerParty();
        int score = party.getUnits().stream().mapToInt(Unit::getLevel).sum() * 100;
        score += party.getGold() * 10;
        for (Map.Entry<Items, Integer> entry : party.getInventory().entrySet())
            score += (entry.getKey().getCost() / 2) * 10 * entry.getValue();
        state.addMessage("Final Score: " + score);
        state.addMessage("Your party is ready for PvP. Save it via the client.");
    }

    // ════════════════════════════════ Helper Methods ═════════════════════════════════

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                   phase input prompts                    │
    //      └──────────────────────────────────────────────────────────┘
    private void addPhasePrompt(CampaignState state) {
        switch (state.getPhase()) {
            case OVERWORLD -> state.addMessage("Actions: [next] [use-item] [level-up] [view-party] [quit]");
            case INN       -> state.addMessage("Inn: [buy <item>] [recruit] [confirm-recruit <index>] [leave]");
            case BATTLE    -> state.addMessage("Battle in progress (battleId: " + state.getActiveBattleSessionId() + "). When done: action=resolve-battle");
            case FINISHED  -> state.addMessage("Campaign finished!");
        }
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │              load campaign state from repo               │
    //      └──────────────────────────────────────────────────────────┘
    private CampaignState loadState(String sessionId) {
        return repo.findById(sessionId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Campaign session not found: " + sessionId)
        ).getState();
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │               save campaign state to repo                │
    //      └──────────────────────────────────────────────────────────┘
    private void saveState(String sessionId, CampaignState state) {
        CampaignSessionEntity entity = repo.findById(sessionId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Campaign session not found: " + sessionId)
        );
        entity.updateState(state); //update the retrieved session with the updated state
        repo.save(entity);
    }
}
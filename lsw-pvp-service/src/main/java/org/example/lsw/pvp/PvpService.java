package org.example.lsw.pvp;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service for initiating a battle between two player-controlled parties
 * in a PVP match.
 * Essentially a kind of wrapper for the battle service
 */
@Service
public class PvpService {
    private final PvpMatchRepository repo; //repo for storing in-progress pvp battles
    private final BattleServiceClient battleClient;
    private final UserServiceClient userClient;

    public PvpService(PvpMatchRepository repo, BattleServiceClient battleClient, UserServiceClient userClient) {
        this.repo = repo;
        this.battleClient = battleClient;
        this.userClient = userClient;
    }

    /**
     * Creates a pvp match, then delegates the battle to battle-service,
     * and returns the match with the battleSessionId for the client to use.
     */
    public PvpMatchResponse startMatch(StartPvpRequest req) {
        String matchId = UUID.randomUUID().toString();

        PvpMatchEntity match = new PvpMatchEntity(
            matchId,
            req.getPlayer1Username(), req.getPlayer1Party().getName(),
            req.getPlayer2Username(), req.getPlayer2Party().getName()
        );

        //start up a battle session in battle-service, and link it to this match
        String battleId = battleClient.startBattle(matchId, req.getPlayer1Party(), req.getPlayer2Party());
        match.setBattleSessionId(battleId);
        repo.save(match);

        return PvpMatchResponse.from(match);
    }

    /**
     * Returns the current match state
     */
    public PvpMatchResponse getMatch(String matchId) {
        return PvpMatchResponse.from(getOrThrow(matchId));
    }

    /**
     * Called by the client when the battle is finished.
     * gets the result from battle-service, records W/L on both user profiles,
     * and saves the revived parties back to their pvp roster slots
     */
    public PvpMatchResponse resolveMatch(String matchId) {
        PvpMatchEntity match = getOrThrow(matchId);

        if (match.isFinished())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Match already resolved.");

        //fetch result from battle-service
        JsonNode result = battleClient.getBattleResult(match.getBattleSessionId());
        String winnerPartyName = result.get("winnerPartyName").asText();

        //determine which player won based on party names
        boolean p1Won = winnerPartyName.equals(match.getPlayer1PartyName());
        String winnerUsername = p1Won ? match.getPlayer1Username() : match.getPlayer2Username();
        String loserUsername = p1Won ? match.getPlayer2Username() : match.getPlayer1Username();

        //record W/L on user-service
        userClient.recordPvpResult(winnerUsername, true);
        userClient.recordPvpResult(loserUsername, false);

        //parse revived parties and save them back to user rosters
        //player 1's party is always partyA (set by startMatch)
        try {
            Party p1Party = GameMapper.get().treeToValue(result.get("partyA"), Party.class);
            Party p2Party = GameMapper.get().treeToValue(result.get("partyB"), Party.class);

            //find the slot index for each party in their owner's roster
            //we save at slot 0 on default. Real slot management is actually in user-service
            userClient.replacePvpParty(match.getPlayer1Username(), 0, p1Party);
            userClient.replacePvpParty(match.getPlayer2Username(), 0, p2Party);
        } catch (Exception e) {
            System.err.println("Warning: failed to save parties back to roster: " + e.getMessage());
        }

        //delete the battle session of this match
        battleClient.deleteBattle(match.getBattleSessionId());

        //set the match as finished
        match.setWinnerUsername(winnerUsername);
        match.setFinished(true);
        repo.save(match);

        return PvpMatchResponse.from(match);
    }

    /**
     * Returns all unfinished matches
     */
    public List<PvpMatchResponse> getActiveMatches() {
        return repo.findAll().stream()
                .filter(m -> !m.isFinished())
                .map(PvpMatchResponse::from)
                .toList();
    }

    private PvpMatchEntity getOrThrow(String matchId) {
        return repo.findById(matchId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "PvP match not found: " + matchId)
        );
    }
}

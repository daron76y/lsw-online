package org.example.lsw.campaign;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;
import org.example.lsw.core.battle.BattleState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for calling battle-service from campaign-service.
 * Uses plain RestTemplate, so no service discovery is needed since
 * URLs are configured in the application.yml and overridden by Docker Compose.
 * Campaign-service only communicates with battle-service through this
 * intermediary client, who handles all HTTP redirections and delegations between
 * the two.
 */
@Component
public class BattleServiceClient {
    private final RestTemplate rest = new RestTemplate();

    @Value("${services.battle-url}")
    private String battleUrl;

    /**
     * Creates a new battle session in battle-service
     * Returns the battle session ID
     */
    public String startBattle(String campaignSessionId, Party playerParty, Party enemyParty) {
        //create the request body
        Map<String, Object> body = Map.of(
            "ownerId", campaignSessionId,
            "ownerType", "PVE",
            "partyA", playerParty,
            "partyB", enemyParty
        );

        //create the http headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //create the http request using the head and serialized body, and post it to localhost:8084/api/battle/start
        String json = GameMapper.toJson(body);
        ResponseEntity<String> response = rest.postForEntity(
            battleUrl + "/api/battle/start",
            new HttpEntity<>(json, headers),
            String.class
        );

        //return the id of the battle that was just started
        try {
            JsonNode node = GameMapper.get().readTree(response.getBody());
            return node.get("battleId").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse battle start response", e);
        }
    }

    /**
     * Gets the final result from a finished battle.
     * Returns the BattleState with revived parties.
     * TODO: party should not revive in pve
     */
    public BattleState getBattleResult(String battleId) {
        //get battle result from the battle service
        ResponseEntity<String> response = rest.getForEntity(
            battleUrl + "/api/battle/" + battleId + "/result",
            String.class
        );

        //return a battleState object from the response entity
        try {
            JsonNode node = GameMapper.get().readTree(response.getBody());
            //reconstruct both parties from the response
            Party partyA = GameMapper.get().treeToValue(node.get("partyA"), Party.class);
            Party partyB = GameMapper.get().treeToValue(node.get("partyB"), Party.class);
            String winner = node.get("winnerPartyName").asText();

            //create the battleState object
            BattleState state = new BattleState(partyA, partyB);
            state.setFinished(true);
            state.setWinnerPartyName(winner);
            return state;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse battle result", e);
        }
    }

    /** deletes the battle session */
    public void deleteBattle(String battleId) {
        rest.delete(battleUrl + "/api/battle/" + battleId);
    }
}

package org.example.lsw.pvp;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for calling battle service from pvp service.
 */
@Component
public class BattleServiceClient {
    private final RestTemplate rest = new RestTemplate();

    @Value("${services.battle-url}")
    private String battleUrl;

    public String startBattle(String matchId, Party p1Party, Party p2Party) {
        Map<String, Object> body = Map.of(
            "ownerId", matchId,
            "ownerType", "PVP",
            "partyA", p1Party,
            "partyB", p2Party
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        //post to the battle-service
        ResponseEntity<String> resp = rest.postForEntity(
            battleUrl + "/api/battle/start",
            new HttpEntity<>(GameMapper.toJson(body), headers),
            String.class
        );
        try {
            return GameMapper.get().readTree(resp.getBody()).get("battleId").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse battle start response", e);
        }
    }

    /** Returns winnerPartyName and both revived parties */
    public JsonNode getBattleResult(String battleId) {
        ResponseEntity<String> resp = rest.getForEntity(
            battleUrl + "/api/battle/" + battleId + "/result",
            String.class
        );
        try {
            return GameMapper.get().readTree(resp.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse battle result", e);
        }
    }

    public void deleteBattle(String battleId) {
        rest.delete(battleUrl + "/api/battle/" + battleId);
    }
}

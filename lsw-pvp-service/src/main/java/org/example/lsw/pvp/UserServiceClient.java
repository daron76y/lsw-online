package org.example.lsw.pvp;

import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for calling user-service from pvp-service
 * Used to record W/L results and save revived parties back to player pvp rosters
 */
@Component
public class UserServiceClient {
    private final RestTemplate rest = new RestTemplate();

    @Value("${services.user-url}")
    private String userUrl;

    public void recordPvpResult(String username, boolean won) {
        rest.postForEntity(
            userUrl + "/api/users/" + username + "/pvp-result?won=" + won,
            null,
            Void.class
        );
    }

    public void replacePvpParty(String username, int slot, Party party) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = GameMapper.toJson(party);
        rest.exchange(
            userUrl + "/api/users/" + username + "/pvp-party/" + slot,
            HttpMethod.PUT,
            new HttpEntity<>(json, headers),
            Void.class
        );
    }
}

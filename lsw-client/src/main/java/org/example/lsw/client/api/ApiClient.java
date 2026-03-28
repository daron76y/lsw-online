package org.example.lsw.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.lsw.core.GameMapper;
import org.example.lsw.core.Party;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * All the HTTP calls that the user makes goes through this api client
 * which redirects it to the other services across the Docker network
 */
public class ApiClient {
    public static final String GATEWAY_URL = System.getProperty("gateway.url", "http://localhost:8080");
    private static final ObjectMapper MAPPER = GameMapper.get();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // ════════════════════════════ Authentication Requests ════════════════════════════
    public JsonNode register(String username, String password) {
        ObjectNode body = MAPPER.createObjectNode()
                .put("username", username)
                .put("password", password);
        return post("/api/auth/register", body);
    }

    public JsonNode login(String username, String password) {
        ObjectNode body = MAPPER.createObjectNode()
                .put("username", username)
                .put("password", password);
        return post("/api/auth/login", body);
    }

    // ═════════════════════════════ User Profile Requests ═════════════════════════════
    public JsonNode getProfile(String username) {
        return get("/api/users/" + username + "/profile");
    }

    public void saveCampaignProgress(String username, String campaignName, String partyName, int currentRoom) {
        ObjectNode body = MAPPER.createObjectNode()
                .put("campaignName", campaignName)
                .put("partyName", partyName)
                .put("currentRoom", currentRoom);
        post("/api/users/" + username + "/campaign", body);
    }

    public void deleteCampaignProgress(String username, String campaignName) {
        delete("/api/users/" + encode(username) + "/campaign/" + encode(campaignName));
    }

    public void saveParty(String username, Party party) {
        post("/api/users/" + username + "/party", toNode(party));
    }

    public void deleteParty(String username, String partyName) {
        delete("/api/users/" + encode(username) + "/party/" + encode(partyName));
    }

    public void savePvpParty(String username, Party party) {
        post("/api/users/" + encode(username) + "/pvp-party", toNode(party));
    }

    public void replacePvpParty(String username, int slot, Party party) {
        put("/api/users/" + username + "/pvp-party/" + slot, toNode(party));
    }

    public void addScore(String username, int points) {
        postEmpty("/api/users/" + username + "/score?points=" + points);
    }

    // ═══════════════════════════════ Party Operations ════════════════════════════════
    //TODO: unneccessary. March 27.
    public JsonNode levelUp(Party party, String unitName, String heroClass) {
        ObjectNode body = MAPPER.createObjectNode()
                .put("unitName", unitName)
                .put("heroClass", heroClass);
        body.set("party", toNode(party));
        return post("/api/parties/level-up", body);
    }

    public JsonNode useItem(Party party, String itemName, String unitName) {
        ObjectNode body = MAPPER.createObjectNode()
                .put("itemName", itemName)
                .put("unitName", unitName);
        body.set("party", toNode(party));
        return post("/api/parties/use-item", body);
    }

    public JsonNode getRecruits() {
        return get("/api/parties/recruits");
    }

    public JsonNode confirmRecruit(Party party, JsonNode recruit) {
        ObjectNode body = MAPPER.createObjectNode();
        body.set("party", toNode(party));
        body.set("recruit", recruit);
        return post("/api/parties/recruit", body);
    }

    // ═══════════════════════════════ Campaign Requests ═══════════════════════════════
    public JsonNode startCampaign(String username, Party party) {
        return startCampaign(username, party, 0);
    }

    public JsonNode startCampaign(String username, Party party, int startRoom) {
        ObjectNode body = MAPPER.createObjectNode()
                .put("username", username)
                .put("startRoom", startRoom);
        body.set("party", toNode(party));
        return post("/api/campaign/start", body);
    }

    public JsonNode getCampaignState(String sessionId) {
        return get("/api/campaign/" + sessionId);
    }

    public JsonNode campaignAction(String sessionId, String action) {
        return campaignAction(sessionId, action, null, null, null, null);
    }

    public JsonNode campaignAction(String sessionId, String action, String itemName, String unitName, String heroClass, Integer recruitIndex) {
        ObjectNode body = MAPPER.createObjectNode().put("action", action);
        if (itemName != null) body.put("itemName", itemName);
        if (unitName != null) body.put("unitName", unitName);
        if (heroClass != null) body.put("heroClass", heroClass);
        if (recruitIndex != null) body.put("recruitIndex", recruitIndex);
        return post("/api/campaign/" + sessionId + "/action", body);
    }

    // ════════════════════════════ Battle Engine Requests ═════════════════════════════
    public JsonNode getBattleState(String battleId) {
        return get("/api/battle/" + battleId);
    }

    public JsonNode battleAction(String battleId, String action, String targetName, String abilityName) {
        ObjectNode body = MAPPER.createObjectNode().put("action", action);
        if (targetName != null) body.put("targetName", targetName);
        if (abilityName != null) body.put("abilityName", abilityName);
        return post("/api/battle/" + battleId + "/action", body);
    }

    // ══════════════════════════════ Pvp Match Requestd ═══════════════════════════════
    public JsonNode startPvpMatch(String p1Username, Party p1Party, String p2Username, Party p2Party) {
        ObjectNode body = MAPPER.createObjectNode()
                .put("player1Username", p1Username)
                .put("player2Username", p2Username);
        body.set("player1Party", toNode(p1Party));
        body.set("player2Party", toNode(p2Party));
        return post("/api/pvp/match/start", body);
    }

    public JsonNode getPvpMatch(String matchId) {
        return get("/api/pvp/match/" + matchId);
    }

    public JsonNode resolvePvpMatch(String matchId) {
        return post("/api/pvp/match/" + matchId + "/resolve", MAPPER.createObjectNode());
    }

    // ═════════════════════════════════ Http Helpers ══════════════════════════════════
    private static String encode(String s) {
        try {return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");}
        catch (Exception e) { return s; }
    }

    private JsonNode get(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL + path))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            return send(req);
        } catch (ApiException e) {throw e;}
        catch (Exception e) {throw new ApiException("Network error: " + e.getMessage());}
    }

    private JsonNode post(String path, Object bodyObj) {
        try {
            String json = MAPPER.writeValueAsString(bodyObj);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            return send(req);
        } catch (ApiException e) {throw e;}
        catch (Exception e) {throw new ApiException("Network error: " + e.getMessage());}
    }

    private void postEmpty(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL + path))
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) throwError(resp);
        } catch (ApiException e) {throw e;}
        catch (Exception e) {throw new ApiException("Network error: " + e.getMessage());}
    }

    private void put(String path, Object bodyObj) {
        try {
            String json = MAPPER.writeValueAsString(bodyObj);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) throwError(resp);
        } catch (ApiException e) {throw e;}
        catch (Exception e) {throw new ApiException("Network error: " + e.getMessage());}
    }

    private void delete(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL + path))
                    .timeout(Duration.ofSeconds(10))
                    .DELETE()
                    .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) throwError(resp);
        } catch (ApiException e) {throw e;}
        catch (Exception e) {throw new ApiException("Network error: " + e.getMessage());}
    }

    private JsonNode send(HttpRequest req) throws Exception {
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throwError(resp);
        return MAPPER.readTree(resp.body());
    }

    private void throwError(HttpResponse<String> resp) {
        try {
            JsonNode node = MAPPER.readTree(resp.body());
            String msg = node.has("message") ? node.get("message").asText()
                    : node.has("error") ? node.get("error").asText()
                    : resp.body();
            throw new ApiException(msg);
        } catch (ApiException e) {throw e;}
        catch (Exception e) {throw new ApiException("HTTP " + resp.statusCode());}
    }

    private JsonNode toNode(Object obj) {
        return MAPPER.valueToTree(obj);
    }
}
package org.example.lsw.campaign;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the campaign service.
 * Gateway routes /api/campaign/__ here (On port 8083).
 * Endpoints:
 *   POST   /api/campaign/start       - create a new campaign session
 *   GET    /api/campaign/{id}        - get current campaign state + prompt
 *   POST   /api/campaign/{id}/action - submit one overworld/inn action
 *   DELETE /api/campaign/{id}       - delete a finished campaign session
 * Battle turns are handled directly by battle service (/api/battle/__).
 * When a battle finishes, the client calls action=resolve-battle here to
 * process rewards/penalties for the player and return to the overworld.
 */
@RestController
@RequestMapping("/api/campaign")
public class CampaignController {
    private final CampaignService service;

    public CampaignController(CampaignService service) {
        this.service = service;
    }

    //create new campaign
    @PostMapping("/start")
    public ResponseEntity<CampaignStateResponse> start(@RequestBody StartCampaignRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startCampaign(req));
    }

    //get current state and input prompt message
    @GetMapping("/{sessionId}")
    public ResponseEntity<CampaignStateResponse> getState(@PathVariable String sessionId) {
        return ResponseEntity.ok(service.getState(sessionId));
    }

    //submit an overworld or inn action
    @PostMapping("/{sessionId}/action")
    public ResponseEntity<CampaignStateResponse> action(@PathVariable String sessionId, @RequestBody CampaignActionRequest req) {
        return ResponseEntity.ok(service.processAction(sessionId, req));
    }

    //delete a campaign session
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> delete(@PathVariable String sessionId) {
        // TODO: implement a deleteSession() method in CampaignService
        return ResponseEntity.noContent().build(); //placeholder. Probably will implement if time permits.
    }
}


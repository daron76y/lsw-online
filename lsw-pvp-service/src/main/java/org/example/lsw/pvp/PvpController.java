package org.example.lsw.pvp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for pvp-service
 * Gateway routes /api/pvp/** here (port 8085)
 * Endpoints
 *   POST /api/pvp/match/start        --> create match + create battle session
 *   GET  /api/pvp/match/{id}         --> get match data
 *   POST /api/pvp/match/{id}/resolve --> finalize: save win/loss records, save parties
 *   GET  /api/pvp/matches/active     --> list unfinished matches
 * Battle turns go directly to battle-service (at /api/battle/{battleId}/action)
 * The client gets battleSessionId from the start response and uses it there
 */
@RestController
@RequestMapping("/api/pvp")
public class PvpController {
    private final PvpService service;

    public PvpController(PvpService service) {
        this.service = service;
    }

    //start a pvp match and battle
    @PostMapping("/match/start")
    public ResponseEntity<PvpMatchResponse> startMatch(@RequestBody StartPvpRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startMatch(req));
    }

    //get the id of this match
    @GetMapping("/match/{matchId}")
    public ResponseEntity<PvpMatchResponse> getMatch(@PathVariable String matchId) {
        return ResponseEntity.ok(service.getMatch(matchId));
    }

    //finish this match
    @PostMapping("/match/{matchId}/resolve")
    public ResponseEntity<PvpMatchResponse> resolveMatch(@PathVariable String matchId) {
        return ResponseEntity.ok(service.resolveMatch(matchId));
    }

    //get all currently active matches
    @GetMapping("/matches/active")
    public ResponseEntity<List<PvpMatchResponse>> getActiveMatches() {
        return ResponseEntity.ok(service.getActiveMatches());
    }
}

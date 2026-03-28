package org.example.lsw.party;

import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for party-service.
 * Gateway routes /api/parties/__ here (port 8082).
 * Endpoints:
 *   POST /api/parties/level-up --> level up a unit's class
 *   POST /api/parties/use-item --> use an item on a unit
 *   GET  /api/parties/recruits --> generate recruit candidates
 *   POST /api/parties/recruit  --> add chosen recruit to party
 *   POST /api/parties/view     --> get formatted party summary
 */
@RestController
@RequestMapping("/api/parties")
public class PartyController {
    private final PartyService service;

    public PartyController(PartyService service) {
        this.service = service;
    }

    //level up unit
    @PostMapping("/level-up")
    public ResponseEntity<PartyResponse> levelUp(@RequestBody LevelUpRequest req) {
        return ResponseEntity.ok(service.levelUp(req));
    }

    //use an item
    @PostMapping("/use-item")
    public ResponseEntity<PartyResponse> useItem(@RequestBody UseItemRequest req) {
        return ResponseEntity.ok(service.useItem(req));
    }

    //generate recruits (for Inns)
    @GetMapping("/recruits")
    public ResponseEntity<List<Unit>> getRecruits() {
        return ResponseEntity.ok(service.generateRecruits());
    }

    //hire a recruit to the party
    @PostMapping("/recruit")
    public ResponseEntity<PartyResponse> recruit(@RequestBody RecruitRequest req) {
        return ResponseEntity.ok(service.confirmRecruit(req));
    }

    //view the party info
    @PostMapping("/view")
    public ResponseEntity<PartyResponse> viewParty(@RequestBody Party party) {
        return ResponseEntity.ok(service.viewParty(party));
    }
}

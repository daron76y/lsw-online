package org.example.lsw.battle;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for our battle service.
 * The gateway routes /api/battle/__ here (on port 8084)
 */
@RestController
@RequestMapping("/api/battle")
public class BattleController {
    private final BattleService service;

    public BattleController(BattleService service) {
        this.service = service;
    }

    // creates a new battle session
    @PostMapping("/start")
    public ResponseEntity<BattleStateResponse> start(@RequestBody StartBattleRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startBattle(req));
    }

    // gets the current state of the battle
    @GetMapping("/{battleId}")
    public ResponseEntity<BattleStateResponse> getState(@PathVariable String battleId) {
        return ResponseEntity.ok(service.getState(battleId));
    }

    // submits a battle command action to a battle session, using its id
    @PostMapping("/{battleId}/action")
    public ResponseEntity<BattleStateResponse> action(@PathVariable String battleId, @RequestBody BattleActionRequest req) {
        return ResponseEntity.ok(service.processAction(battleId, req));
    }

    // gets the final result of a battle after it has naturally concluded
    @GetMapping("/{battleId}/result")
    public ResponseEntity<BattleStateResponse> result(@PathVariable String battleId) {
        return ResponseEntity.ok(service.getFinishedResult(battleId));
    }

    // deletes the battle session from the database (after it's over)
    @DeleteMapping("/{battleId}")
    public ResponseEntity<Void> delete(@PathVariable String battleId) {
        service.deleteBattle(battleId);
        return ResponseEntity.noContent().build();
    }
}

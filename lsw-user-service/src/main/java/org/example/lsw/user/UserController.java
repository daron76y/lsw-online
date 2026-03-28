package org.example.lsw.user;

import org.example.lsw.core.Party;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user-service.
 * All routes are prefixed with /api/auth or /api/users.
 * The gateway routes /api/auth/__ and /api/users/__ here.
 * Endpoints:
 *   POST   /api/auth/register
 *   POST   /api/auth/login
 *   GET    /api/users/{username}/profile
 *   POST   /api/users/{username}/campaign         --> update/insert a campaign save
 *   DELETE /api/users/{username}/campaign/{name}  --> delete a campaign save
 *   POST   /api/users/{username}/party            --> update/insert a saved party
 *   DELETE /api/users/{username}/party/{name}     --> delete a saved party
 *   POST   /api/users/{username}/pvp-party        --> add a PVP party
 *   PUT    /api/users/{username}/pvp-party/{slot} --> replace a PVP party
 *   POST   /api/users/{username}/pvp-result       --> record win or loss
 *   POST   /api/users/{username}/score            --> add campaign score points
 */
@RestController
public class UserController {
    private final UserService service;
    public UserController(UserService service) {
        this.service = service;
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      authentication                      │
    //      └──────────────────────────────────────────────────────────┘
    @PostMapping("/api/auth/register")
    public ResponseEntity<UserProfileDto> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(req));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<UserProfileDto> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(service.login(req));
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      profile stuff                       │
    //      └──────────────────────────────────────────────────────────┘
    @GetMapping("/api/users/{username}/profile")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable String username) {
        return ResponseEntity.ok(service.getProfile(username));
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      campaign saves                      │
    //      └──────────────────────────────────────────────────────────┘
    @PostMapping("/api/users/{username}/campaign")
    public ResponseEntity<Void> saveCampaign(@PathVariable String username, @RequestBody CampaignProgressDto progress) {
        service.saveCampaignProgress(username, progress);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/users/{username}/campaign/{campaignName}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable String username, @PathVariable String campaignName) {
        service.deleteCampaignProgress(username, campaignName);
        return ResponseEntity.ok().build();
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                       pve parties                        │
    //      └──────────────────────────────────────────────────────────┘
    @PostMapping("/api/users/{username}/party")
    public ResponseEntity<Void> saveParty(@PathVariable String username, @RequestBody Party party) {
        service.saveParty(username, party);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/users/{username}/party/{partyName}")
    public ResponseEntity<Void> deleteParty(@PathVariable String username, @PathVariable String partyName) {
        service.deleteParty(username, partyName);
        return ResponseEntity.ok().build();
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                       pvp parties                        │
    //      └──────────────────────────────────────────────────────────┘
    @PostMapping("/api/users/{username}/pvp-party")
    public ResponseEntity<Void> savePvpParty(@PathVariable String username, @RequestBody Party party) {
        service.savePvpParty(username, party);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/users/{username}/pvp-party/{slot}")
    public ResponseEntity<Void> replacePvpParty(@PathVariable String username, @PathVariable int slot, @RequestBody Party party) {
        service.replacePvpParty(username, slot, party);
        return ResponseEntity.ok().build();
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                   pvp win/loss records                   │
    //      └──────────────────────────────────────────────────────────┘
    @PostMapping("/api/users/{username}/pvp-result")
    public ResponseEntity<Void> recordPvpResult(@PathVariable String username, @RequestParam boolean won) {
        service.recordPvpResult(username, won);
        return ResponseEntity.ok().build();
    }

    //      ┌──────────────────────────────────────────────────────────┐
    //      │                      campaign score                      │
    //      └──────────────────────────────────────────────────────────┘
    @PostMapping("/api/users/{username}/score")
    public ResponseEntity<Void> addScore(@PathVariable String username, @RequestParam int points) {
        service.addScore(username, points);
        return ResponseEntity.ok().build();
    }
}

package org.example.lsw.user;

import org.example.lsw.core.Party;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    // ════════════════════════════════ Authentication ═════════════════════════════════
    public UserProfileDto register(RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty.");
        if (req.getPassword() == null || req.getPassword().length() < 4)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 4 characters.");
        if (repo.existsByUsername(req.getUsername()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken.");

        UserEntity user = new UserEntity(req.getUsername(), req.getPassword());
        repo.save(user);
        return new UserProfileDto(user);
    }

    public UserProfileDto login(LoginRequest req) {
        UserEntity user = repo.findById(req.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No account found."));
        if (!user.getPassword().equals(req.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect password.");
        return new UserProfileDto(user);
    }

    // ════════════════════════════════ Profile Methods ════════════════════════════════
    public UserProfileDto getProfile(String username) {
        return new UserProfileDto(getOrThrow(username));
    }

    // ═══════════════════════════════════ Campaign ════════════════════════════════════
    public void saveCampaignProgress(String username, CampaignProgressDto progress) {
        UserEntity user = getOrThrow(username);
        List<CampaignProgressDto> saves = new ArrayList<>(user.getCampaignSaves());

        //update/insert, overwrite existing entry with the same name
        saves.removeIf(c -> c.getCampaignName().equals(progress.getCampaignName()));
        saves.add(progress);
        user.setCampaignSaves(saves);

        repo.save(user);
    }

    public void deleteCampaignProgress(String username, String campaignName) {
        UserEntity user = getOrThrow(username);
        List<CampaignProgressDto> saves = new ArrayList<>(user.getCampaignSaves());
        saves.removeIf(c -> c.getCampaignName().equals(campaignName));
        user.setCampaignSaves(saves);
        repo.save(user);
    }

    // ═══════════════════════════════ Party Management ════════════════════════════════
    public void saveParty(String username, Party party) {
        UserEntity user = getOrThrow(username);
        List<Party> parties = new ArrayList<>(user.getSavedParties());
        parties.removeIf(p -> p.getName().equals(party.getName())); //update/insert
        parties.add(party);
        user.setSavedParties(parties);
        repo.save(user);
    }

    public void deleteParty(String username, String partyName) {
        UserEntity user = getOrThrow(username);
        List<Party> parties = new ArrayList<>(user.getSavedParties());
        parties.removeIf(p -> p.getName().equals(partyName));
        user.setSavedParties(parties);
        repo.save(user);
    }

    // ══════════════════════════════════ Pvp Parties ══════════════════════════════════
    public void savePvpParty(String username, Party party) {
        UserEntity user = getOrThrow(username);
        List<Party> parties = new ArrayList<>(user.getPvpParties());
        if (parties.size() >= 5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PVP roster is full (max 5)");
        parties.removeIf(p -> p.getName().equals(party.getName())); //update/insert
        parties.add(party);
        user.setPvpParties(parties);
        repo.save(user);
    }

    public void replacePvpParty(String username, int slot, Party party) {
        UserEntity user = getOrThrow(username);
        List<Party> parties = new ArrayList<>(user.getPvpParties());
        if (slot < 0 || slot >= parties.size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slot: " + slot);
        parties.set(slot, party);
        user.setPvpParties(parties);
        repo.save(user);
    }

    // ═════════════════════════════ Pvp Win/loss Records ══════════════════════════════
    public void recordPvpResult(String username, boolean won) {
        UserEntity user = getOrThrow(username);
        if (won) user.setPvpWins(user.getPvpWins() + 1);
        else user.setPvpLosses(user.getPvpLosses() + 1);
        repo.save(user);
    }

    // ════════════════════════════════ Campaign Score ═════════════════════════════════
    public void addScore(String username, int points) {
        UserEntity user = getOrThrow(username);
        user.setScore(user.getScore() + points);
        repo.save(user);
    }

    // ════════════════════════════════════ Helper ═════════════════════════════════════
    private UserEntity getOrThrow(String username) {
        return repo.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }
}

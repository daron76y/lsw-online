package org.example.lsw.user;

import org.example.lsw.core.HeroClass;
import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repo;

    @InjectMocks
    private UserService service;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserEntity("testuser", "password123");
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");

        when(repo.existsByUsername("newuser")).thenReturn(false);
        when(repo.save(any(UserEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        UserProfileDto result = service.register(request);

        assertEquals("newuser", result.getUsername());
        verify(repo, times(1)).save(any(UserEntity.class));
    }

    @Test
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");

        when(repo.existsByUsername("existing")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> {
            service.register(request);
        });
    }

    @Test
    void testRegisterInvalidPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user");
        request.setPassword("123"); // Too short

        assertThrows(ResponseStatusException.class, () -> {
            service.register(request);
        });
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));

        UserProfileDto result = service.login(request);

        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testLoginWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(ResponseStatusException.class, () -> {
            service.login(request);
        });
    }

    @Test
    void testLoginUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("password");

        when(repo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            service.login(request);
        });
    }

    @Test
    void testSaveParty() {
        Party party = new Party("Test Party");
        party.addUnit(new Unit("Hero", HeroClass.WARRIOR));

        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));
        when(repo.save(any(UserEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> {
            service.saveParty("testuser", party);
        });

        verify(repo, times(1)).save(testUser);
    }

    @Test
    void testSavePvpParty() {
        Party party = new Party("PvP Party");
        party.addUnit(new Unit("Hero", HeroClass.WARRIOR));

        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));
        when(repo.save(any(UserEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> {
            service.savePvpParty("testuser", party);
        });

        verify(repo, times(1)).save(testUser);
    }

    @Test
    void testRecordPvpResult() {
        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));
        when(repo.save(any(UserEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.recordPvpResult("testuser", true);
        assertEquals(1, testUser.getPvpWins());

        service.recordPvpResult("testuser", false);
        assertEquals(1, testUser.getPvpLosses());

        verify(repo, times(2)).save(testUser);
    }

    @Test
    void testAddScore() {
        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));
        when(repo.save(any(UserEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.addScore("testuser", 1000);

        assertEquals(1000, testUser.getScore());
        verify(repo, times(1)).save(testUser);
    }

    @Test
    void testSaveCampaignProgress() {
        CampaignProgressDto progress = new CampaignProgressDto(
                "Test Campaign", "Test Party", 5
        );

        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));
        when(repo.save(any(UserEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.saveCampaignProgress("testuser", progress);

        assertEquals(1, testUser.getCampaignSaves().size());
        verify(repo, times(1)).save(testUser);
    }

    @Test
    void testDeleteCampaignProgress() {
        CampaignProgressDto progress = new CampaignProgressDto(
                "Test Campaign", "Test Party", 5
        );
        testUser.getCampaignSaves().add(progress);

        when(repo.findById("testuser")).thenReturn(Optional.of(testUser));
        when(repo.save(any(UserEntity.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.deleteCampaignProgress("testuser", "Test Campaign");

        assertTrue(testUser.getCampaignSaves().isEmpty());
        verify(repo, times(1)).save(testUser);
    }
}
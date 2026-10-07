package com.unime.securegame.controller;

import com.unime.securegame.model.PlayerProfile;
import com.unime.securegame.repository.PlayerProfileRepository;
import com.unime.securegame.service.TOTPService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerProfileController {

    private final PlayerProfileRepository playerProfileRepository;
    private final TOTPService totpService;

    public PlayerProfileController(PlayerProfileRepository playerProfileRepository, TOTPService totpService) {
        this.playerProfileRepository = playerProfileRepository;
        this.totpService = totpService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public List<PlayerView> getAllPlayers() {
        return playerProfileRepository.findAll().stream().map(PlayerView::from).toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and #request != null and #request.username != null and #request.username.trim() == authentication.name")
    public ResponseEntity<?> createPlayer(@RequestBody PlayerCreateRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.username().trim().length() > 80) {
            return ResponseEntity.badRequest().body("Invalid username");
        }
        // Bind only the public username, never caller-supplied counters or an MFA secret.
        PlayerProfile player = new PlayerProfile(request.username().trim());
        player.setTotpSecret(totpService.generateSecret());
        return ResponseEntity.ok(PlayerView.from(playerProfileRepository.save(player)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlayerView> getPlayer(@PathVariable Long id, Authentication authentication) {
        return playerProfileRepository.findById(id).map(player -> {
            boolean privileged = authentication.getAuthorities().stream().anyMatch(authority ->
                    "ROLE_TEACHER".equals(authority.getAuthority()) || "ROLE_ADMIN".equals(authority.getAuthority()));
            if (!privileged && !player.getUsername().equals(authentication.getName())) {
                return ResponseEntity.status(403).<PlayerView>build();
            }
            return ResponseEntity.ok(PlayerView.from(player));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/score")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<PlayerView> updateScore(@PathVariable Long id, @RequestParam int delta) {
        if (delta < -10_000 || delta > 10_000) return ResponseEntity.badRequest().build();
        return playerProfileRepository.findById(id).map(player -> {
            long updatedScore = (long) player.getScore() + delta;
            if (updatedScore < 0 || updatedScore > 1_000_000) return ResponseEntity.badRequest().<PlayerView>build();
            player.setScore((int) updatedScore);
            return ResponseEntity.ok(PlayerView.from(playerProfileRepository.save(player)));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        if (!playerProfileRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        playerProfileRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    public record PlayerCreateRequest(String username) {}

    public record PlayerView(Long id, String username, int score, int badgeCount,
                             java.time.Instant createdAt) {
        static PlayerView from(PlayerProfile player) {
            return new PlayerView(player.getId(), player.getUsername(), player.getScore(),
                    player.getBadgeCount(), player.getCreatedAt());
        }
    }
}

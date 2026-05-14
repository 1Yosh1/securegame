package com.unime.securegame.controller;

import com.unime.securegame.model.PlayerProfile;
import com.unime.securegame.repository.PlayerProfileRepository;
import com.unime.securegame.service.TOTPService;
import org.springframework.http.ResponseEntity;
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
    public List<PlayerProfile> getAllPlayers() {
        return playerProfileRepository.findAll();
    }

    @PostMapping
    public PlayerProfile createPlayer(@RequestBody PlayerProfile player) {
        // Auto-generate TOTP secret on registration
        player.setTotpSecret(totpService.generateSecret());
        return playerProfileRepository.save(player);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlayerProfile> getPlayer(@PathVariable Long id) {
        return playerProfileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/score")
    public ResponseEntity<PlayerProfile> updateScore(@PathVariable Long id, @RequestParam int delta) {
        return playerProfileRepository.findById(id).map(player -> {
            player.setScore(player.getScore() + delta);
            return ResponseEntity.ok(playerProfileRepository.save(player));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        if (!playerProfileRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        playerProfileRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

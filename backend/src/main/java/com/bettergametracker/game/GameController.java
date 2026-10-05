package com.bettergametracker.game;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<GameResponse> list() {
        return gameService.list().stream().map(GameController::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<GameResponse> create(@Valid @RequestBody GameRequest request) {
        GameResponse response = toResponse(gameService.create(toGame(request)));
        return ResponseEntity.created(URI.create("/api/v1/games/" + response.id())).body(response);
    }

    @GetMapping("/{gameId}")
    public GameResponse get(@PathVariable UUID gameId) {
        return toResponse(gameService.get(gameId));
    }

    @PutMapping("/{gameId}")
    public GameResponse update(@PathVariable UUID gameId, @Valid @RequestBody GameRequest request) {
        return toResponse(gameService.update(gameId, toGame(request)));
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> delete(@PathVariable UUID gameId) {
        gameService.delete(gameId);
        return ResponseEntity.noContent().build();
    }

    private static Game toGame(GameRequest request) {
        Game game = new Game(request.gameTitle());
        game.setDescription(request.description());
        game.setReleasePlatform(request.releasePlatform());
        game.setReleaseDate(request.releaseDate());
        game.setDeveloper(request.developer());
        game.setMetaRating(request.metaRating());
        game.setUserRating(request.userRating());
        game.setPhysicalCopy(request.physicalCopy());
        return game;
    }

    private static GameResponse toResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getGameTitle(),
                game.getDescription(),
                game.getReleasePlatform(),
                game.getReleaseDate(),
                game.getDeveloper(),
                game.getMetaRating(),
                game.getUserRating(),
                game.getPhysicalCopy());
    }
}

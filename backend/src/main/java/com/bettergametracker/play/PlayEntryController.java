package com.bettergametracker.play;

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
@RequestMapping("/api/v1/games/{gameId}/plays")
public class PlayEntryController {

    private final PlayEntryService playEntryService;

    public PlayEntryController(PlayEntryService playEntryService) {
        this.playEntryService = playEntryService;
    }

    @GetMapping
    public List<PlayEntryResponse> list(@PathVariable UUID gameId) {
        return playEntryService.list(gameId).stream().map(PlayEntryController::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<PlayEntryResponse> create(@PathVariable UUID gameId,
            @Valid @RequestBody PlayEntryRequest request) {
        PlayEntryResponse response = toResponse(playEntryService.create(gameId, toPlayEntry(request)));
        return ResponseEntity.created(URI.create("/api/v1/games/" + gameId + "/plays/" + response.id()))
                .body(response);
    }

    @GetMapping("/{playId}")
    public PlayEntryResponse get(@PathVariable UUID gameId, @PathVariable UUID playId) {
        return toResponse(playEntryService.get(gameId, playId));
    }

    @PutMapping("/{playId}")
    public PlayEntryResponse update(@PathVariable UUID gameId, @PathVariable UUID playId,
            @Valid @RequestBody PlayEntryRequest request) {
        return toResponse(playEntryService.update(gameId, playId, toPlayEntry(request)));
    }

    @DeleteMapping("/{playId}")
    public ResponseEntity<Void> delete(@PathVariable UUID gameId, @PathVariable UUID playId) {
        playEntryService.delete(gameId, playId);
        return ResponseEntity.noContent().build();
    }

    private static PlayEntry toPlayEntry(PlayEntryRequest request) {
        PlayEntry playEntry = new PlayEntry();
        playEntry.setPlaythroughRating(request.playthroughRating());
        playEntry.setCompletionDate(request.completionDate());
        playEntry.setPlatformPlayedOn(request.platformPlayedOn());
        playEntry.setTimeToBeat(request.timeToBeat());
        playEntry.setCompletionRate(request.completionRate());
        playEntry.setCoop(request.coop());
        playEntry.setLocation(request.location());
        return playEntry;
    }

    private static PlayEntryResponse toResponse(PlayEntry playEntry) {
        return new PlayEntryResponse(
                playEntry.getId(),
                playEntry.getGame().getId(),
                playEntry.getPlaythroughRating(),
                playEntry.getCompletionDate(),
                playEntry.getPlatformPlayedOn(),
                playEntry.getTimeToBeat(),
                playEntry.getCompletionRate(),
                playEntry.getCoop(),
                playEntry.getLocation());
    }
}

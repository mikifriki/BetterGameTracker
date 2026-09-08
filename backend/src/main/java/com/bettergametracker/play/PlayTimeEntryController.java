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
@RequestMapping("/api/v1/games/{gameId}/plays/{playId}/time-entries")
public class PlayTimeEntryController {

    private final PlayTimeEntryService timeEntryService;

    public PlayTimeEntryController(PlayTimeEntryService timeEntryService) {
        this.timeEntryService = timeEntryService;
    }

    @GetMapping
    public List<PlayTimeEntryResponse> list(@PathVariable UUID gameId, @PathVariable UUID playId) {
        return timeEntryService.list(gameId, playId).stream().map(PlayTimeEntryController::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<PlayTimeEntryResponse> create(@PathVariable UUID gameId, @PathVariable UUID playId,
            @Valid @RequestBody PlayTimeEntryRequest request) {
        PlayTimeEntryResponse response = toResponse(timeEntryService.create(gameId, playId, toPlayTimeEntry(request)));
        return ResponseEntity.created(URI.create("/api/v1/games/" + gameId + "/plays/" + playId
                + "/time-entries/" + response.id())).body(response);
    }

    @PutMapping("/{timeEntryId}")
    public PlayTimeEntryResponse update(@PathVariable UUID gameId, @PathVariable UUID playId,
            @PathVariable UUID timeEntryId, @Valid @RequestBody PlayTimeEntryRequest request) {
        return toResponse(timeEntryService.update(gameId, playId, timeEntryId, toPlayTimeEntry(request)));
    }

    @DeleteMapping("/{timeEntryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID gameId, @PathVariable UUID playId,
            @PathVariable UUID timeEntryId) {
        timeEntryService.delete(gameId, playId, timeEntryId);
        return ResponseEntity.noContent().build();
    }

    private static PlayTimeEntry toPlayTimeEntry(PlayTimeEntryRequest request) {
        PlayTimeEntry timeEntry = new PlayTimeEntry();
        timeEntry.setDate(request.date());
        timeEntry.setDurationMinutes(request.durationMinutes());
        timeEntry.setNotes(request.notes());
        return timeEntry;
    }

    private static PlayTimeEntryResponse toResponse(PlayTimeEntry timeEntry) {
        return new PlayTimeEntryResponse(timeEntry.getId(), timeEntry.getPlayEntry().getId(),
                timeEntry.getDate(), timeEntry.getDurationMinutes(), timeEntry.getNotes());
    }
}

package com.bettergametracker.play;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayTimeEntryService {

    private final PlayTimeEntryRepository timeEntryRepository;
    private final PlayEntryService playEntryService;

    public PlayTimeEntryService(PlayTimeEntryRepository timeEntryRepository, PlayEntryService playEntryService) {
        this.timeEntryRepository = timeEntryRepository;
        this.playEntryService = playEntryService;
    }

    @Transactional
    public PlayTimeEntry create(UUID gameId, UUID playEntryId, PlayTimeEntry timeEntry) {
        PlayEntry playEntry = playEntryService.get(gameId, playEntryId);
        playEntry.addTimeEntry(timeEntry);
        return timeEntryRepository.save(timeEntry);
    }

    public List<PlayTimeEntry> list(UUID gameId, UUID playEntryId) {
        playEntryService.get(gameId, playEntryId);
        return timeEntryRepository.findAllByPlayEntry_Id(playEntryId);
    }

    public PlayTimeEntry get(UUID gameId, UUID playEntryId, UUID timeEntryId) {
        playEntryService.get(gameId, playEntryId);
        return timeEntryRepository.findByIdAndPlayEntry_Id(timeEntryId, playEntryId)
                .orElseThrow(() -> new PlayTimeEntryNotFoundException(playEntryId, timeEntryId));
    }

    @Transactional
    public PlayTimeEntry update(UUID gameId, UUID playEntryId, UUID timeEntryId, PlayTimeEntry replacement) {
        PlayTimeEntry timeEntry = get(gameId, playEntryId, timeEntryId);
        timeEntry.setDate(replacement.getDate());
        timeEntry.setDurationMinutes(replacement.getDurationMinutes());
        timeEntry.setNotes(replacement.getNotes());
        return timeEntry;
    }

    @Transactional
    public void delete(UUID gameId, UUID playEntryId, UUID timeEntryId) {
        PlayTimeEntry timeEntry = get(gameId, playEntryId, timeEntryId);
        timeEntry.getPlayEntry().removeTimeEntry(timeEntry);
    }
}

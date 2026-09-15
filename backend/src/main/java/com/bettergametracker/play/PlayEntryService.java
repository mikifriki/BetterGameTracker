package com.bettergametracker.play;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bettergametracker.game.Game;
import com.bettergametracker.game.GameService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlayEntryService {

    private final PlayEntryRepository playEntryRepository;
    private final GameService gameService;
    private final PlayTimeEntryRepository timeEntryRepository;

    public PlayEntryService(PlayEntryRepository playEntryRepository, GameService gameService,
            PlayTimeEntryRepository timeEntryRepository) {
        this.playEntryRepository = playEntryRepository;
        this.gameService = gameService;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional
    public PlayEntrySummary create(UUID gameId, PlayEntry playEntry) {
        Game game = gameService.get(gameId);
        if (playEntry.getStartDate() == null) {
            playEntry.setStartDate(LocalDate.now());
        }
        game.addPlayEntry(playEntry);
        return new PlayEntrySummary(playEntryRepository.save(playEntry), 0);
    }

    public List<PlayEntrySummary> list(UUID gameId) {
        gameService.get(gameId);
        return playEntryRepository.findSummariesByGameId(gameId);
    }

    public PlayEntry get(UUID gameId, UUID playEntryId) {
        gameService.get(gameId);
        return playEntryRepository.findByIdAndGame_Id(playEntryId, gameId)
                .orElseThrow(() -> new PlayEntryNotFoundException(gameId, playEntryId));
    }

    public PlayEntrySummary getWithTime(UUID gameId, UUID playEntryId) {
        return new PlayEntrySummary(get(gameId, playEntryId), timeEntryRepository.sumDurationByPlayEntryId(playEntryId));
    }

    @Transactional
    public PlayEntrySummary update(UUID gameId, UUID playEntryId, PlayEntry replacement) {
        PlayEntry playEntry = get(gameId, playEntryId);
        playEntry.setPlaythroughRating(replacement.getPlaythroughRating());
        playEntry.setStartDate(replacement.getStartDate());
        playEntry.setCompletionDate(replacement.getCompletionDate());
        playEntry.setPlatformPlayedOn(replacement.getPlatformPlayedOn());
        playEntry.setTimeToBeatMinutes(replacement.getTimeToBeatMinutes());
        playEntry.setCompletionStatus(replacement.getCompletionStatus());
        playEntry.setCoop(replacement.getCoop());
        playEntry.setLocation(replacement.getLocation());
        return new PlayEntrySummary(playEntry, timeEntryRepository.sumDurationByPlayEntryId(playEntryId));
    }

    @Transactional
    public void delete(UUID gameId, UUID playEntryId) {
        PlayEntry playEntry = get(gameId, playEntryId);
        playEntry.getGame().removePlayEntry(playEntry);
    }
}

package com.bettergametracker.play;

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

    public PlayEntryService(PlayEntryRepository playEntryRepository, GameService gameService) {
        this.playEntryRepository = playEntryRepository;
        this.gameService = gameService;
    }

    @Transactional
    public PlayEntry create(UUID gameId, PlayEntry playEntry) {
        Game game = gameService.get(gameId);
        game.addPlayEntry(playEntry);
        return playEntryRepository.save(playEntry);
    }

    public List<PlayEntry> list(UUID gameId) {
        gameService.get(gameId);
        return playEntryRepository.findAllByGame_Id(gameId);
    }

    public PlayEntry get(UUID gameId, UUID playEntryId) {
        gameService.get(gameId);
        return playEntryRepository.findByIdAndGame_Id(playEntryId, gameId)
                .orElseThrow(() -> new PlayEntryNotFoundException(gameId, playEntryId));
    }

    @Transactional
    public PlayEntry update(UUID gameId, UUID playEntryId, PlayEntry replacement) {
        PlayEntry playEntry = get(gameId, playEntryId);
        playEntry.setPlaythroughRating(replacement.getPlaythroughRating());
        playEntry.setCompletionDate(replacement.getCompletionDate());
        playEntry.setPlatformPlayedOn(replacement.getPlatformPlayedOn());
        playEntry.setTimeToBeat(replacement.getTimeToBeat());
        playEntry.setCompletionRate(replacement.getCompletionRate());
        playEntry.setCoop(replacement.getCoop());
        playEntry.setLocation(replacement.getLocation());
        return playEntry;
    }

    @Transactional
    public void delete(UUID gameId, UUID playEntryId) {
        PlayEntry playEntry = get(gameId, playEntryId);
        playEntry.getGame().removePlayEntry(playEntry);
        playEntryRepository.delete(playEntry);
    }
}

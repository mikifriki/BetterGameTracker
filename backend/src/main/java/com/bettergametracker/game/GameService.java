package com.bettergametracker.game;

import java.util.List;
import java.util.UUID;

import com.bettergametracker.security.CurrentUser;
import com.bettergametracker.cover.CoverStorage;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GameService {

    private final GameRepository gameRepository;

    private final CurrentUser currentUser;
    private final CoverStorage coverStorage;

    public GameService(GameRepository gameRepository, CurrentUser currentUser, CoverStorage coverStorage) {
        this.gameRepository = gameRepository;
        this.currentUser = currentUser;
        this.coverStorage = coverStorage;
    }

    @Transactional
    public Game create(Game game) {
        game.assignOwner(currentUser.id());
        return gameRepository.save(game);
    }

    public Game get(UUID id) {
        return gameRepository.findByIdAndOwnerId(id, currentUser.id()).orElseThrow(() -> new GameNotFoundException(id));
    }

    @Transactional
    public Game lock(UUID id) {
        return gameRepository.findWithLockByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new GameNotFoundException(id));
    }

    public List<Game> list() {
        return gameRepository.findAllByOwnerId(currentUser.id(),
                Sort.by(Sort.Order.asc("gameTitle").ignoreCase(), Sort.Order.asc("id")));
    }

    @Transactional
    public Game update(UUID id, Game replacement) {
        Game game = get(id);
        game.setGameTitle(replacement.getGameTitle());
        game.setDescription(replacement.getDescription());
        game.setReleasePlatform(replacement.getReleasePlatform());
        game.setReleaseDate(replacement.getReleaseDate());
        game.setDeveloper(replacement.getDeveloper());
        game.setMetaRating(replacement.getMetaRating());
        game.setUserRating(replacement.getUserRating());
        game.setPhysicalCopy(replacement.getPhysicalCopy());
        return game;
    }

    @Transactional
    public void delete(UUID id) {
        gameRepository.delete(get(id));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    coverStorage.delete(id);
                } catch (java.io.IOException exception) {
                    org.slf4j.LoggerFactory.getLogger(GameService.class)
                            .error("Could not remove cover for deleted game {}", id, exception);
                }
            }
        });
    }
}

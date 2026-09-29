package com.bettergametracker.cover;

import java.io.IOException;
import java.util.UUID;
import com.bettergametracker.game.GameService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CoverService {
    private final GameService games;
    private final CoverStorage storage;

    public CoverService(GameService games, CoverStorage storage) {
        this.games = games;
        this.storage = storage;
    }

    public byte[] get(UUID gameId) throws IOException {
        games.get(gameId);
        return storage.read(gameId);
    }

    @Transactional
    public void put(UUID gameId, byte[] bytes) throws IOException {
        games.lock(gameId);
        storage.write(gameId, bytes);
    }

    @Transactional
    public void delete(UUID gameId) throws IOException {
        games.lock(gameId);
        storage.delete(gameId);
    }
}

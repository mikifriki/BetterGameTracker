package com.bettergametracker.game;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Game> findWithLockByIdAndOwnerId(UUID id, UUID ownerId);

    List<Game> findAllByOwnerId(UUID ownerId);

    Optional<Game> findByIdAndOwnerId(UUID id, UUID ownerId);
}

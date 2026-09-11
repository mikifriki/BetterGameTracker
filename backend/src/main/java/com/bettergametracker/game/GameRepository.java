package com.bettergametracker.game;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Game> findWithLockByIdAndOwnerId(UUID id, UUID ownerId);

    List<Game> findAllByOwnerId(UUID ownerId, org.springframework.data.domain.Sort sort);

    Optional<Game> findByIdAndOwnerId(UUID id, UUID ownerId);
}

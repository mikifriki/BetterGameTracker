package com.bettergametracker.play;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayEntryRepository extends JpaRepository<PlayEntry, UUID> {

    List<PlayEntry> findAllByGame_Id(UUID gameId);

    Optional<PlayEntry> findByIdAndGame_Id(UUID id, UUID gameId);
}

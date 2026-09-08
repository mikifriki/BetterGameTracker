package com.bettergametracker.play;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayTimeEntryRepository extends JpaRepository<PlayTimeEntry, UUID> {

    List<PlayTimeEntry> findAllByPlayEntry_Id(UUID playEntryId);
    Optional<PlayTimeEntry> findByIdAndPlayEntry_Id(UUID id, UUID playEntryId);
}

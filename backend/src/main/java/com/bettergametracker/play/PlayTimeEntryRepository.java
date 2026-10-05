package com.bettergametracker.play;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlayTimeEntryRepository extends JpaRepository<PlayTimeEntry, UUID> {

    @Query("""
            select t from PlayTimeEntry t where t.playEntry.id = :playEntryId order by t.date desc, t.id
            """)
    List<PlayTimeEntry> findAllByPlayEntry_Id(UUID playEntryId);

    @Query("""
            select coalesce(sum(t.durationMinutes), 0L) from PlayTimeEntry t where t.playEntry.id = :playEntryId
            """)
    long sumDurationByPlayEntryId(UUID playEntryId);

    Optional<PlayTimeEntry> findByIdAndPlayEntry_Id(UUID id, UUID playEntryId);
}

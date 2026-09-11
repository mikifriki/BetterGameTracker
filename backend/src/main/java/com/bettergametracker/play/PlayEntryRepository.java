package com.bettergametracker.play;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlayEntryRepository extends JpaRepository<PlayEntry, UUID> {

    @Query("""
            select new com.bettergametracker.play.PlayEntrySummary(p, coalesce(sum(t.durationMinutes), 0L))
            from PlayEntry p left join p.timeEntries t
            where p.game.id = :gameId
            group by p
            order by case when p.completionDate is null then 1 else 0 end, p.completionDate desc, p.id
            """)
    List<PlayEntrySummary> findSummariesByGameId(UUID gameId);

    Optional<PlayEntry> findByIdAndGame_Id(UUID id, UUID gameId);
}

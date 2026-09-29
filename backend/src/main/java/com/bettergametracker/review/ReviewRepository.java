package com.bettergametracker.review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("""
            select r from Review r where r.playEntry.id = :playEntryId
            order by case when r.reviewDate is null then 1 else 0 end, r.reviewDate desc, r.id
            """)
    List<Review> findAllByPlayEntry_Id(UUID playEntryId);

    Optional<Review> findByIdAndPlayEntry_Id(UUID id, UUID playEntryId);
}

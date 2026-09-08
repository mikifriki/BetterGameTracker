package com.bettergametracker.review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findAllByPlayEntry_Id(UUID playEntryId);

    Optional<Review> findByIdAndPlayEntry_Id(UUID id, UUID playEntryId);
}

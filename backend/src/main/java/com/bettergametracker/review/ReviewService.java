package com.bettergametracker.review;

import java.util.List;
import java.util.UUID;

import com.bettergametracker.play.PlayEntry;
import com.bettergametracker.play.PlayEntryService;
import jakarta.persistence.Persistence;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlayEntryService playEntryService;

    public ReviewService(ReviewRepository reviewRepository, PlayEntryService playEntryService) {
        this.reviewRepository = reviewRepository;
        this.playEntryService = playEntryService;
    }

    @Transactional
    public Review create(UUID gameId, UUID playEntryId, Review review) {
        PlayEntry playEntry = playEntryService.get(gameId, playEntryId);
        playEntry.addReview(review);
        return reviewRepository.save(review);
    }

    public List<Review> list(UUID gameId, UUID playEntryId) {
        playEntryService.get(gameId, playEntryId);
        return reviewRepository.findAllByPlayEntry_Id(playEntryId);
    }

    public Review get(UUID gameId, UUID playEntryId, UUID reviewId) {
        playEntryService.get(gameId, playEntryId);
        return reviewRepository.findByIdAndPlayEntry_Id(reviewId, playEntryId)
                .orElseThrow(() -> new ReviewNotFoundException(playEntryId, reviewId));
    }

    @Transactional
    public Review update(UUID gameId, UUID playEntryId, UUID reviewId, Review replacement) {
        Review review = get(gameId, playEntryId, reviewId);
        review.setReviewDate(replacement.getReviewDate());
        review.setReviewTitle(replacement.getReviewTitle());
        review.setReview(replacement.getReview());
        review.setRating(replacement.getRating());
        return review;
    }

    @Transactional
    public void delete(UUID gameId, UUID playEntryId, UUID reviewId) {
        Review review = get(gameId, playEntryId, reviewId);
        if (Persistence.getPersistenceUtil().isLoaded(review.getPlayEntry(), "reviews")) {
            review.getPlayEntry().removeReview(review);
        }
        reviewRepository.delete(review);
    }
}

package com.bettergametracker.review;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games/{gameId}/plays/{playId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ReviewResponse> list(@PathVariable UUID gameId, @PathVariable UUID playId) {
        return reviewService.list(gameId, playId).stream().map(ReviewController::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@PathVariable UUID gameId, @PathVariable UUID playId,
            @RequestBody ReviewRequest request) {
        ReviewResponse response = toResponse(reviewService.create(gameId, playId, toReview(request)));
        return ResponseEntity.created(URI.create("/api/v1/games/" + gameId + "/plays/" + playId
                + "/reviews/" + response.id())).body(response);
    }

    @GetMapping("/{reviewId}")
    public ReviewResponse get(@PathVariable UUID gameId, @PathVariable UUID playId,
            @PathVariable UUID reviewId) {
        return toResponse(reviewService.get(gameId, playId, reviewId));
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse update(@PathVariable UUID gameId, @PathVariable UUID playId,
            @PathVariable UUID reviewId, @RequestBody ReviewRequest request) {
        return toResponse(reviewService.update(gameId, playId, reviewId, toReview(request)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> delete(@PathVariable UUID gameId, @PathVariable UUID playId,
            @PathVariable UUID reviewId) {
        reviewService.delete(gameId, playId, reviewId);
        return ResponseEntity.noContent().build();
    }

    private static Review toReview(ReviewRequest request) {
        Review review = new Review();
        review.setReviewDate(request.reviewDate());
        review.setReviewTitle(request.reviewTitle());
        review.setReview(request.review());
        review.setRating(request.rating());
        return review;
    }

    private static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(review.getId(), review.getPlayEntry().getId(), review.getReviewDate(),
                review.getReviewTitle(), review.getReview(), review.getRating());
    }
}

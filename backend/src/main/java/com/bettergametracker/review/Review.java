package com.bettergametracker.review;

import java.util.UUID;

import com.bettergametracker.play.PlayEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String reviewDate;

    private String reviewTitle;

    @Column(name = "review_text")
    private String review;

    private String rating;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "play_entry_id", nullable = false)
    private PlayEntry playEntry;

    /**
     * The first PlayEntry this review was attached to during its current entity lifetime.
     * Ownership is immutable, but a null play entry is still allowed while a parent removes the review.
     */
    @jakarta.persistence.Transient
    private PlayEntry initialPlayEntry;

    public Review() {
        // Required by JPA.
    }

    public UUID getId() {
        return id;
    }

    public String getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(String reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getReviewTitle() {
        return reviewTitle;
    }

    public void setReviewTitle(String reviewTitle) {
        this.reviewTitle = reviewTitle;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public PlayEntry getPlayEntry() {
        return playEntry;
    }

    /** Updates only the owning side; use the parent add/remove methods to manage the relationship. */
    public void assignPlayEntry(PlayEntry playEntry) {
        if (samePlayEntry(this.playEntry, playEntry)) {
            return;
        }
        if (initialPlayEntry == null) {
            initialPlayEntry = this.playEntry;
        }
        if (playEntry != null && initialPlayEntry != null && !samePlayEntry(initialPlayEntry, playEntry)) {
            throw new IllegalStateException("A review cannot be reassigned to a different play entry");
        }
        this.playEntry = playEntry;
    }

    private static boolean samePlayEntry(PlayEntry first, PlayEntry second) {
        if (first == second) {
            return true;
        }
        UUID firstId = first == null ? null : first.getId();
        UUID secondId = second == null ? null : second.getId();
        return firstId != null && firstId.equals(secondId);
    }
}

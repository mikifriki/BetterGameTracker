package com.bettergametracker.play;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.bettergametracker.game.Game;
import com.bettergametracker.review.Review;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "play_entries")
public class PlayEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The Go GameEntryDetails.Id value, retained separately from the UUID primary key.
     */
    @Column
    private Integer legacyId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String playthroughRating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String completionDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String platformPlayedOn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String timeToBeat;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String completionRate;

    @Column(columnDefinition = "TEXT")
    private String coop;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * The first Game this play entry was attached to during its current entity lifetime.
     * Ownership is immutable, but a null game is still allowed while a parent removes the entry.
     */
    @jakarta.persistence.Transient
    private Game initialGame;

    @OneToMany(mappedBy = "playEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "playEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayTimeEntry> timeEntries = new ArrayList<>();

    public PlayEntry() {
        // Required by JPA.
    }

    public UUID getId() {
        return id;
    }

    public Integer getLegacyId() {
        return legacyId;
    }

    public void setLegacyId(Integer legacyId) {
        this.legacyId = Objects.requireNonNull(legacyId, "legacyId must not be null");
    }

    public String getPlaythroughRating() {
        return playthroughRating;
    }

    public void setPlaythroughRating(String playthroughRating) {
        this.playthroughRating = Objects.requireNonNull(playthroughRating, "playthroughRating must not be null");
    }

    public String getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(String completionDate) {
        this.completionDate = Objects.requireNonNull(completionDate, "completionDate must not be null");
    }

    public String getPlatformPlayedOn() {
        return platformPlayedOn;
    }

    public void setPlatformPlayedOn(String platformPlayedOn) {
        this.platformPlayedOn = Objects.requireNonNull(platformPlayedOn, "platformPlayedOn must not be null");
    }

    public String getTimeToBeat() {
        return timeToBeat;
    }

    public void setTimeToBeat(String timeToBeat) {
        this.timeToBeat = Objects.requireNonNull(timeToBeat, "timeToBeat must not be null");
    }

    public String getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(String completionRate) {
        this.completionRate = Objects.requireNonNull(completionRate, "completionRate must not be null");
    }

    public String getCoop() {
        return coop;
    }

    public void setCoop(String coop) {
        this.coop = coop;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = Objects.requireNonNull(location, "location must not be null");
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        assignGame(Objects.requireNonNull(game, "game must not be null"));
    }

    public List<Review> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    public void addReview(Review review) {
        Review requiredReview = Objects.requireNonNull(review, "review must not be null");
        if (requiredReview.getPlayEntry() != this) {
            requiredReview.assignPlayEntry(this);
        }
        if (!reviews.contains(requiredReview)) {
            reviews.add(requiredReview);
        }
    }

    public void removeReview(Review review) {
        if (review == null) {
            return;
        }
        if (review.getPlayEntry() == this) {
            review.assignPlayEntry(null);
        } else {
            reviews.remove(review);
        }
    }

    public List<PlayTimeEntry> getTimeEntries() {
        return Collections.unmodifiableList(timeEntries);
    }

    public void addTimeEntry(PlayTimeEntry timeEntry) {
        PlayTimeEntry requiredTimeEntry = Objects.requireNonNull(timeEntry, "timeEntry must not be null");
        if (requiredTimeEntry.getPlayEntry() != this) {
            requiredTimeEntry.assignPlayEntry(this);
        }
        if (!timeEntries.contains(requiredTimeEntry)) {
            timeEntries.add(requiredTimeEntry);
        }
    }

    public void removeTimeEntry(PlayTimeEntry timeEntry) {
        if (timeEntry == null) {
            return;
        }
        if (timeEntry.getPlayEntry() == this) {
            timeEntry.assignPlayEntry(null);
        } else {
            timeEntries.remove(timeEntry);
        }
    }

    /**
     * Synchronizes the owning Game relation. A null value is used only while removing a play entry.
     */
    public void assignGame(Game game) {
        if (sameGame(this.game, game)) {
            return;
        }
        if (game != null) {
            Game establishedGame = initialGame != null ? initialGame : this.game;
            if (establishedGame != null && !sameGame(establishedGame, game)) {
                throw new IllegalStateException("A play entry cannot be reassigned to a different game");
            }
            if (initialGame == null) {
                initialGame = establishedGame != null ? establishedGame : game;
            }
        } else if (initialGame == null && this.game != null) {
            initialGame = this.game;
        }
        Game previousGame = this.game;
        this.game = game;
        if (previousGame != null) {
            previousGame.removePlayEntry(this);
        }
        if (game != null) {
            game.addPlayEntry(this);
        }
    }

    private static boolean sameGame(Game first, Game second) {
        if (first == second) {
            return true;
        }
        UUID firstId = first == null ? null : first.getId();
        UUID secondId = second == null ? null : second.getId();
        return firstId != null && firstId.equals(secondId);
    }
}

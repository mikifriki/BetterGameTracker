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

    @Column(nullable = false)
    private String playthroughRating;

    @Column(nullable = false)
    private String completionDate;

    @Column(nullable = false)
    private String platformPlayedOn;

    private String timeToBeat;

    @Column(nullable = false)
    private String completionRate;

    private String coop;

    @Column(nullable = false)
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
        this.timeToBeat = timeToBeat;
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

    public List<Review> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    public void addReview(Review review) {
        review.assignPlayEntry(this);
        if (!reviews.contains(review)) {
            reviews.add(review);
        }
    }

    public void removeReview(Review review) {
        if (reviews.remove(review)) {
            review.assignPlayEntry(null);
        }
    }

    public List<PlayTimeEntry> getTimeEntries() {
        return Collections.unmodifiableList(timeEntries);
    }

    public void addTimeEntry(PlayTimeEntry timeEntry) {
        timeEntry.assignPlayEntry(this);
        if (!timeEntries.contains(timeEntry)) {
            timeEntries.add(timeEntry);
        }
    }

    public void removeTimeEntry(PlayTimeEntry timeEntry) {
        if (timeEntries.remove(timeEntry)) {
            timeEntry.assignPlayEntry(null);
        }
    }

    /**
     * Updates only the owning side; use Game.addPlayEntry/removePlayEntry to manage the relationship.
     */
    public void assignGame(Game game) {
        if (sameGame(this.game, game)) {
            return;
        }
        if (initialGame == null) {
            initialGame = this.game;
        }
        if (game != null && initialGame != null && !sameGame(initialGame, game)) {
            throw new IllegalStateException("A play entry cannot be reassigned to a different game");
        }
        this.game = game;
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

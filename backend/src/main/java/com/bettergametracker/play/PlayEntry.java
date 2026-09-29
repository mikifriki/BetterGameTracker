package com.bettergametracker.play;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.bettergametracker.game.Game;
import com.bettergametracker.review.Review;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Column(precision = 3, scale = 1, columnDefinition = "DECIMAL(3,1)")
    private BigDecimal playthroughRating;

    private LocalDate startDate;

    private LocalDate completionDate;

    private String platformPlayedOn;

    private Integer timeToBeatMinutes;

    @Enumerated(EnumType.STRING)
    private CompletionStatus completionStatus;

    private Boolean coop;

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
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getPlaythroughRating() {
        return playthroughRating;
    }

    public void setPlaythroughRating(BigDecimal playthroughRating) {
        this.playthroughRating = playthroughRating;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public String getPlatformPlayedOn() {
        return platformPlayedOn;
    }

    public void setPlatformPlayedOn(String platformPlayedOn) {
        this.platformPlayedOn = platformPlayedOn;
    }

    public Integer getTimeToBeatMinutes() {
        return timeToBeatMinutes;
    }

    public void setTimeToBeatMinutes(Integer timeToBeatMinutes) {
        this.timeToBeatMinutes = timeToBeatMinutes;
    }

    public CompletionStatus getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(CompletionStatus completionStatus) {
        this.completionStatus = completionStatus;
    }

    public Boolean getCoop() {
        return coop;
    }

    public void setCoop(Boolean coop) {
        this.coop = coop;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Game getGame() {
        return game;
    }

    public List<Review> getReviews() {
        return Collections.unmodifiableList(reviews);
    }

    public void addReview(Review review) {
        boolean unattached = review.getPlayEntry() == null;
        review.assignPlayEntry(this);
        if (unattached || !reviews.contains(review)) {
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
        boolean unattached = timeEntry.getPlayEntry() == null;
        timeEntry.assignPlayEntry(this);
        if (unattached || !timeEntries.contains(timeEntry)) {
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

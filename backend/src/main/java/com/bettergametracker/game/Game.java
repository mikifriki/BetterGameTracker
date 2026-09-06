package com.bettergametracker.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.bettergametracker.play.PlayEntry;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String hrefTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String gameTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String releasePlatform;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String releaseDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String developer;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metaRating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userRating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String physicalCopy;

    /**
     * Legacy cover reference metadata. Cover storage is intentionally outside this entity.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String coverImage;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlayEntry> playEntries = new ArrayList<>();

    protected Game() {
        // Required by JPA.
    }

    public Game(String gameTitle) {
        this.gameTitle = Objects.requireNonNull(gameTitle, "gameTitle must not be null");
    }

    public UUID getId() {
        return id;
    }

    public String getHrefTitle() {
        return hrefTitle;
    }

    public void setHrefTitle(String hrefTitle) {
        this.hrefTitle = hrefTitle;
    }

    public String getGameTitle() {
        return gameTitle;
    }

    public void setGameTitle(String gameTitle) {
        this.gameTitle = Objects.requireNonNull(gameTitle, "gameTitle must not be null");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNull(description, "description must not be null");
    }

    public String getReleasePlatform() {
        return releasePlatform;
    }

    public void setReleasePlatform(String releasePlatform) {
        this.releasePlatform = Objects.requireNonNull(releasePlatform, "releasePlatform must not be null");
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = Objects.requireNonNull(releaseDate, "releaseDate must not be null");
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = Objects.requireNonNull(developer, "developer must not be null");
    }

    public String getMetaRating() {
        return metaRating;
    }

    public void setMetaRating(String metaRating) {
        this.metaRating = Objects.requireNonNull(metaRating, "metaRating must not be null");
    }

    public String getUserRating() {
        return userRating;
    }

    public void setUserRating(String userRating) {
        this.userRating = Objects.requireNonNull(userRating, "userRating must not be null");
    }

    public String getPhysicalCopy() {
        return physicalCopy;
    }

    public void setPhysicalCopy(String physicalCopy) {
        this.physicalCopy = Objects.requireNonNull(physicalCopy, "physicalCopy must not be null");
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = Objects.requireNonNull(coverImage, "coverImage must not be null");
    }

    public List<PlayEntry> getPlayEntries() {
        return Collections.unmodifiableList(playEntries);
    }

    public void addPlayEntry(PlayEntry playEntry) {
        PlayEntry requiredPlayEntry = Objects.requireNonNull(playEntry, "playEntry must not be null");
        if (requiredPlayEntry.getGame() != this) {
            requiredPlayEntry.assignGame(this);
        }
        if (!playEntries.contains(requiredPlayEntry)) {
            playEntries.add(requiredPlayEntry);
        }
    }

    public void removePlayEntry(PlayEntry playEntry) {
        if (playEntry == null) {
            return;
        }
        if (playEntry.getGame() == this) {
            playEntry.assignGame(null);
        } else {
            playEntries.remove(playEntry);
        }
    }
}

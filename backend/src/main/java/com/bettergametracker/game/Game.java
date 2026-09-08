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

    private UUID ownerId;

    @Column(nullable = false)
    private String gameTitle;

    private String description;

    private String releasePlatform;

    private String releaseDate;

    private String developer;

    private String metaRating;

    private String userRating;

    private String physicalCopy;

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

    void assignOwner(UUID ownerId) {
        this.ownerId = ownerId;
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
        this.description = description;
    }

    public String getReleasePlatform() {
        return releasePlatform;
    }

    public void setReleasePlatform(String releasePlatform) {
        this.releasePlatform = releasePlatform;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getMetaRating() {
        return metaRating;
    }

    public void setMetaRating(String metaRating) {
        this.metaRating = metaRating;
    }

    public String getUserRating() {
        return userRating;
    }

    public void setUserRating(String userRating) {
        this.userRating = userRating;
    }

    public String getPhysicalCopy() {
        return physicalCopy;
    }

    public void setPhysicalCopy(String physicalCopy) {
        this.physicalCopy = physicalCopy;
    }

    public List<PlayEntry> getPlayEntries() {
        return Collections.unmodifiableList(playEntries);
    }

    public void addPlayEntry(PlayEntry playEntry) {
        playEntry.assignGame(this);
        if (!playEntries.contains(playEntry)) {
            playEntries.add(playEntry);
        }
    }

    public void removePlayEntry(PlayEntry playEntry) {
        if (playEntries.remove(playEntry)) {
            playEntry.assignGame(null);
        }
    }
}

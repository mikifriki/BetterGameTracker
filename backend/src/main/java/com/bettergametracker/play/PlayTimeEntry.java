package com.bettergametracker.play;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "play_time_entries")
public class PlayTimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "play_entry_id", nullable = false)
    private PlayEntry playEntry;

    /** The first PlayEntry this time entry was attached to during its current entity lifetime. */
    @jakarta.persistence.Transient
    private PlayEntry initialPlayEntry;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public PlayTimeEntry() {
        // Required by JPA.
    }

    public UUID getId() {
        return id;
    }

    public PlayEntry getPlayEntry() {
        return playEntry;
    }

    public void setPlayEntry(PlayEntry playEntry) {
        assignPlayEntry(Objects.requireNonNull(playEntry, "playEntry must not be null"));
    }

    public void assignPlayEntry(PlayEntry playEntry) {
        if (samePlayEntry(this.playEntry, playEntry)) {
            return;
        }
        if (playEntry != null) {
            PlayEntry establishedPlayEntry = initialPlayEntry != null ? initialPlayEntry : this.playEntry;
            if (establishedPlayEntry != null && !samePlayEntry(establishedPlayEntry, playEntry)) {
                throw new IllegalStateException("A play time entry cannot be reassigned to a different play entry");
            }
            if (initialPlayEntry == null) {
                initialPlayEntry = establishedPlayEntry != null ? establishedPlayEntry : playEntry;
            }
        } else if (initialPlayEntry == null && this.playEntry != null) {
            initialPlayEntry = this.playEntry;
        }
        PlayEntry previousPlayEntry = this.playEntry;
        this.playEntry = playEntry;
        if (previousPlayEntry != null) {
            previousPlayEntry.removeTimeEntry(this);
        }
        if (playEntry != null) {
            playEntry.addTimeEntry(this);
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = Objects.requireNonNull(date, "date must not be null");
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        this.durationMinutes = durationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        if (date == null) {
            throw new IllegalStateException("date must not be null");
        }
        if (durationMinutes <= 0) {
            throw new IllegalStateException("durationMinutes must be positive");
        }
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

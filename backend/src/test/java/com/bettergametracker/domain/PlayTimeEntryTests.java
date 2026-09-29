package com.bettergametracker.domain;

import com.bettergametracker.play.PlayEntry;
import com.bettergametracker.play.PlayTimeEntry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayTimeEntryTests {
    @Test
    void timeEntriesAreOptionalAndOwnershipHelpersSynchronizeBothSides() {
        PlayEntry play = new PlayEntry();
        assertThat(play.getTimeEntries()).isEmpty();
        PlayTimeEntry entry = new PlayTimeEntry();
        play.addTimeEntry(entry);
        play.addTimeEntry(entry);
        assertThat(play.getTimeEntries()).containsExactly(entry);
        assertThat(entry.getPlayEntry()).isSameAs(play);
        play.removeTimeEntry(entry);
        assertThat(play.getTimeEntries()).isEmpty();
        assertThat(entry.getPlayEntry()).isNull();
    }

    @Test
    void timeEntryOwnershipCannotChangeEvenAfterRemoval() {
        PlayEntry original = new PlayEntry();
        PlayEntry other = new PlayEntry();
        PlayTimeEntry entry = new PlayTimeEntry();
        original.addTimeEntry(entry);

        assertThatThrownBy(() -> other.addTimeEntry(entry)).isInstanceOf(IllegalStateException.class);
        other.removeTimeEntry(entry);
        assertThat(entry.getPlayEntry()).isSameAs(original);
        assertThat(original.getTimeEntries()).containsExactly(entry);
        assertThat(other.getTimeEntries()).isEmpty();

        original.removeTimeEntry(entry);
        assertThatThrownBy(() -> other.addTimeEntry(entry)).isInstanceOf(IllegalStateException.class);
        assertThat(entry.getPlayEntry()).isNull();
        assertThat(other.getTimeEntries()).isEmpty();
    }

    @Test
    void durationMustBePositiveAndDateAndOwnerAreRequired() {
        PlayTimeEntry entry = new PlayTimeEntry();
        assertThatThrownBy(() -> entry.setDurationMinutes(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> entry.setDurationMinutes(-10)).isInstanceOf(IllegalArgumentException.class);
        entry.setDurationMinutes(1);
        assertThat(entry.getDurationMinutes()).isEqualTo(1);
        assertThatThrownBy(() -> entry.setDate(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PlayEntry().addTimeEntry(null)).isInstanceOf(NullPointerException.class);
    }
}

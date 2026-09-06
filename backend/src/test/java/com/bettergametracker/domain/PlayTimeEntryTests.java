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
        entry.setPlayEntry(play);
        play.addTimeEntry(entry);
        assertThat(play.getTimeEntries()).containsExactly(entry);
        assertThat(entry.getPlayEntry()).isSameAs(play);
        play.removeTimeEntry(entry);
        assertThat(play.getTimeEntries()).isEmpty();
        assertThat(entry.getPlayEntry()).isNull();
    }

    @Test
    void durationMustBePositiveAndDateAndOwnerAreRequired() {
        PlayTimeEntry entry = new PlayTimeEntry();
        assertThatThrownBy(() -> entry.setDurationMinutes(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> entry.setDurationMinutes(-10)).isInstanceOf(IllegalArgumentException.class);
        entry.setDurationMinutes(1);
        assertThat(entry.getDurationMinutes()).isEqualTo(1);
        assertThatThrownBy(() -> entry.setDate(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> entry.setPlayEntry(null)).isInstanceOf(NullPointerException.class);
    }
}

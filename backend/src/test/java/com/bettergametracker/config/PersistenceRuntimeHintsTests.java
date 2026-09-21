package com.bettergametracker.config;

import com.bettergametracker.play.PlayEntry;
import com.bettergametracker.play.PlayEntrySummary;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceRuntimeHintsTests {

    @Test
    void allowsHibernateToInvokeThePlayEntrySummaryConstructor() throws NoSuchMethodException {
        RuntimeHints hints = new RuntimeHints();
        new PersistenceRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection()
                .onConstructor(PlayEntrySummary.class.getConstructor(PlayEntry.class, long.class)).invoke()
                .test(hints)).isTrue();
    }
}

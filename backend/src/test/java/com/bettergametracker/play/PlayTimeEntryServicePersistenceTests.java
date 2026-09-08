package com.bettergametracker.play;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import com.bettergametracker.game.Game;
import com.bettergametracker.game.GameNotFoundException;
import com.bettergametracker.game.GameService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class PlayTimeEntryServicePersistenceTests {

    private static final Path DATABASE_PATH = createTemporaryDatabasePath();

    @Autowired
    private GameService gameService;

    @Autowired
    private PlayEntryService playEntryService;

    @Autowired
    private PlayTimeEntryService timeEntryService;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void temporarySqliteDatabase(DynamicPropertyRegistry registry) {
        registry.add("BETTER_GAME_TRACKER_DATABASE_PATH", DATABASE_PATH::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
    }

    @Test
    void persistsAllFieldsAndUpdatesWithoutChangingOwnership() {
        Game game = gameService.create(game("Game"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play"));
        UUID id = timeEntryService.create(game.getId(), play.getId(), timeEntry("Original")).getId();
        entityManager.flush();
        entityManager.clear();

        PlayTimeEntry loaded = timeEntryService.get(game.getId(), play.getId(), id);
        assertThat(loaded.getDate()).isEqualTo(LocalDate.of(2026, 9, 8));
        assertThat(loaded.getDurationMinutes()).isEqualTo(30);
        assertThat(loaded.getNotes()).isEqualTo("Original");
        assertThat(loaded.getPlayEntry().getId()).isEqualTo(play.getId());

        PlayTimeEntry replacement = timeEntry("Updated");
        replacement.setDate(LocalDate.of(2026, 9, 9));
        replacement.setDurationMinutes(60);
        replacement.assignPlayEntry(new PlayEntry());
        timeEntryService.update(game.getId(), play.getId(), id, replacement);
        entityManager.flush();
        entityManager.clear();
        loaded = timeEntryService.get(game.getId(), play.getId(), id);
        assertThat(loaded.getDate()).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(loaded.getDurationMinutes()).isEqualTo(60);
        assertThat(loaded.getNotes()).isEqualTo("Updated");
        assertThat(loaded.getPlayEntry().getId()).isEqualTo(play.getId());

        PlayTimeEntry withoutNotes = timeEntry("Unused");
        withoutNotes.setNotes(null);
        timeEntryService.update(game.getId(), play.getId(), id, withoutNotes);
        entityManager.flush();
        entityManager.clear();
        assertThat(timeEntryService.get(game.getId(), play.getId(), id).getNotes()).isNull();
    }

    @Test
    void listsSameDateEntriesAndDeletesOnlyTheTarget() {
        Game game = gameService.create(game("Game"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play"));
        PlayEntry other = playEntryService.create(game.getId(), playEntry("Other"));
        assertThat(timeEntryService.list(game.getId(), play.getId())).isEmpty();
        PlayTimeEntry first = timeEntryService.create(game.getId(), play.getId(), timeEntry("First"));
        PlayTimeEntry second = timeEntryService.create(game.getId(), play.getId(), timeEntry("Second"));
        PlayTimeEntry unrelated = timeEntryService.create(game.getId(), other.getId(), timeEntry("Other"));
        entityManager.flush();
        entityManager.clear();
        assertThat(timeEntryService.list(game.getId(), play.getId())).extracting(PlayTimeEntry::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());

        PlayEntry parent = playEntryService.get(game.getId(), play.getId());
        timeEntryService.delete(game.getId(), play.getId(), first.getId());
        assertThat(parent.getTimeEntries()).extracting(PlayTimeEntry::getId).containsExactly(second.getId());
        entityManager.flush();
        entityManager.clear();
        assertThatThrownBy(() -> timeEntryService.get(game.getId(), play.getId(), first.getId()))
                .isInstanceOf(PlayTimeEntryNotFoundException.class);
        assertThat(timeEntryService.list(game.getId(), play.getId())).extracting(PlayTimeEntry::getId)
                .containsExactly(second.getId());
        assertThat(timeEntryService.get(game.getId(), other.getId(), unrelated.getId()).getNotes())
                .isEqualTo("Other");
    }

    @Test
    void rejectsWrongOrMissingParentsForEveryOperation() {
        Game game = gameService.create(game("Game"));
        Game other = gameService.create(game("Other"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play"));
        UUID id = timeEntryService.create(game.getId(), play.getId(), timeEntry("Original")).getId();
        entityManager.flush();
        entityManager.clear();

        assertParentRejected(other.getId(), play.getId(), id, PlayEntryNotFoundException.class);
        assertParentRejected(game.getId(), UUID.randomUUID(), id, PlayEntryNotFoundException.class);
        assertParentRejected(UUID.randomUUID(), play.getId(), id, GameNotFoundException.class);
        entityManager.flush();
        entityManager.clear();
        assertThat(timeEntryService.get(game.getId(), play.getId(), id).getNotes()).isEqualTo("Original");
    }

    @Test
    void rejectsWrongPlayEntryAndMissingPlayTimeEntriesForGetUpdateAndDelete() {
        Game game = gameService.create(game("Game"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play"));
        PlayEntry other = playEntryService.create(game.getId(), playEntry("Other"));
        UUID id = timeEntryService.create(game.getId(), play.getId(), timeEntry("Original")).getId();
        entityManager.flush();
        entityManager.clear();

        assertPlayTimeEntryRejected(game.getId(), other.getId(), id);
        assertPlayTimeEntryRejected(game.getId(), play.getId(), UUID.randomUUID());
        entityManager.flush();
        entityManager.clear();
        assertThat(timeEntryService.get(game.getId(), play.getId(), id).getNotes()).isEqualTo("Original");
    }

    @Test
    void rejectsCreatingAPlayTimeEntryAlreadyOwnedByAnotherPlayEntry() {
        Game game = gameService.create(game("Game"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play"));
        PlayEntry other = playEntryService.create(game.getId(), playEntry("Other"));
        PlayTimeEntry timeEntry = timeEntryService.create(game.getId(), play.getId(), timeEntry("Original"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> timeEntryService.create(game.getId(), other.getId(), timeEntry))
                .isInstanceOf(IllegalStateException.class);
        assertThat(timeEntryService.list(game.getId(), other.getId())).isEmpty();
        assertThat(timeEntryService.get(game.getId(), play.getId(), timeEntry.getId()).getNotes())
                .isEqualTo("Original");
    }

    private void assertParentRejected(UUID gameId, UUID playId, UUID timeEntryId,
                                      Class<? extends Throwable> exception) {
        assertThatThrownBy(() -> timeEntryService.create(gameId, playId, timeEntry("New"))).isInstanceOf(exception);
        assertThatThrownBy(() -> timeEntryService.list(gameId, playId)).isInstanceOf(exception);
        assertThatThrownBy(() -> timeEntryService.get(gameId, playId, timeEntryId)).isInstanceOf(exception);
        assertThatThrownBy(() -> timeEntryService.update(gameId, playId, timeEntryId, timeEntry("New")))
                .isInstanceOf(exception);
        assertThatThrownBy(() -> timeEntryService.delete(gameId, playId, timeEntryId)).isInstanceOf(exception);
    }

    private void assertPlayTimeEntryRejected(UUID gameId, UUID playId, UUID timeEntryId) {
        assertThatThrownBy(() -> timeEntryService.get(gameId, playId, timeEntryId))
                .isInstanceOf(PlayTimeEntryNotFoundException.class);
        assertThatThrownBy(() -> timeEntryService.update(gameId, playId, timeEntryId, timeEntry("New")))
                .isInstanceOf(PlayTimeEntryNotFoundException.class);
        assertThatThrownBy(() -> timeEntryService.delete(gameId, playId, timeEntryId))
                .isInstanceOf(PlayTimeEntryNotFoundException.class);
    }

    private static PlayTimeEntry timeEntry(String prefix) {
        PlayTimeEntry timeEntry = new PlayTimeEntry();
        timeEntry.setDate(LocalDate.of(2026, 9, 8));
        timeEntry.setDurationMinutes(30);
        timeEntry.setNotes(prefix);
        return timeEntry;
    }

    private static Game game(String title) {
        Game game = new Game(title);
        game.setDescription("Description");
        game.setReleasePlatform("PC");
        game.setReleaseDate("2026-09-07");
        game.setDeveloper("Developer");
        game.setMetaRating("90");
        game.setUserRating("9.0");
        game.setPhysicalCopy("Yes");
        return game;
    }

    private static PlayEntry playEntry(String prefix) {
        PlayEntry playEntry = new PlayEntry();
        playEntry.setPlaythroughRating(prefix + " rating");
        playEntry.setCompletionDate(prefix + " date");
        playEntry.setPlatformPlayedOn(prefix + " platform");
        playEntry.setTimeToBeat(prefix + " time");
        playEntry.setCompletionRate(prefix + " completion");
        playEntry.setCoop(prefix + " coop");
        playEntry.setLocation(prefix + " location");
        return playEntry;
    }

    private static Path createTemporaryDatabasePath() {
        try {
            Path path = java.nio.file.Files.createTempFile("better-game-tracker-time-entries-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not create temporary SQLite database", exception);
        }
    }
}

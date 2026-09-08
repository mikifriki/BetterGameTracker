package com.bettergametracker.play;

import java.nio.file.Path;
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
class PlayEntryServicePersistenceTests {

    private static final Path DATABASE_PATH = createTemporaryDatabasePath();

    @Autowired
    private GameService gameService;

    @Autowired
    private PlayEntryService playEntryService;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void temporarySqliteDatabase(DynamicPropertyRegistry registry) {
        registry.add("BETTER_GAME_TRACKER_DATABASE_PATH", DATABASE_PATH::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
    }

    @Test
    void createsAndLoadsAPlayEntryWithAllScalarFieldsAndItsOwner() {
        Game game = gameService.create(game("Game"));

        UUID playEntryId = playEntryService.create(game.getId(), playEntry("Original")).getId();
        entityManager.flush();
        entityManager.clear();

        PlayEntry loaded = playEntryService.get(game.getId(), playEntryId);
        assertThat(loaded.getId()).isEqualTo(playEntryId);
        assertThat(loaded.getPlaythroughRating()).isEqualTo("Original rating");
        assertThat(loaded.getCompletionDate()).isEqualTo("Original date");
        assertThat(loaded.getPlatformPlayedOn()).isEqualTo("Original platform");
        assertThat(loaded.getTimeToBeat()).isEqualTo("Original time");
        assertThat(loaded.getCompletionRate()).isEqualTo("Original completion");
        assertThat(loaded.getCoop()).isEqualTo("Original coop");
        assertThat(loaded.getLocation()).isEqualTo("Original location");
        assertThat(loaded.getGame().getId()).isEqualTo(game.getId());
    }

    @Test
    void listsOnlyPlayEntriesOwnedByTheRequestedGame() {
        Game firstGame = gameService.create(game("First"));
        Game secondGame = gameService.create(game("Second"));
        PlayEntry first = playEntryService.create(firstGame.getId(), playEntry("First"));
        PlayEntry second = playEntryService.create(firstGame.getId(), playEntry("Second"));
        playEntryService.create(secondGame.getId(), playEntry("Other"));

        assertThat(playEntryService.list(firstGame.getId()))
                .extracting(PlayEntry::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void updatesScalarFieldsWithoutChangingIdentityOrOwnership() {
        Game game = gameService.create(game("Game"));
        UUID playEntryId = playEntryService.create(game.getId(), playEntry("Original")).getId();
        PlayEntry replacement = playEntry("Replacement");
        replacement.setCoop(null);

        PlayEntry updated = playEntryService.update(game.getId(), playEntryId, replacement);
        entityManager.flush();
        entityManager.clear();

        PlayEntry loaded = playEntryService.get(game.getId(), playEntryId);
        assertThat(updated.getId()).isEqualTo(playEntryId);
        assertThat(loaded.getPlaythroughRating()).isEqualTo("Replacement rating");
        assertThat(loaded.getCompletionDate()).isEqualTo("Replacement date");
        assertThat(loaded.getPlatformPlayedOn()).isEqualTo("Replacement platform");
        assertThat(loaded.getTimeToBeat()).isEqualTo("Replacement time");
        assertThat(loaded.getCompletionRate()).isEqualTo("Replacement completion");
        assertThat(loaded.getCoop()).isNull();
        assertThat(loaded.getLocation()).isEqualTo("Replacement location");
        assertThat(loaded.getGame().getId()).isEqualTo(game.getId());
    }

    @Test
    void rejectsCrossGameGetUpdateAndDeleteWithoutChangingTheEntry() {
        Game owner = gameService.create(game("Owner"));
        Game other = gameService.create(game("Other"));
        UUID playEntryId = playEntryService.create(owner.getId(), playEntry("Original")).getId();

        assertThatThrownBy(() -> playEntryService.get(other.getId(), playEntryId))
                .isInstanceOf(PlayEntryNotFoundException.class);
        assertThatThrownBy(() -> playEntryService.update(other.getId(), playEntryId, playEntry("Changed")))
                .isInstanceOf(PlayEntryNotFoundException.class);
        assertThatThrownBy(() -> playEntryService.delete(other.getId(), playEntryId))
                .isInstanceOf(PlayEntryNotFoundException.class);

        PlayEntry retained = playEntryService.get(owner.getId(), playEntryId);
        assertThat(retained.getPlaythroughRating()).isEqualTo("Original rating");
        assertThat(retained.getGame().getId()).isEqualTo(owner.getId());
    }

    @Test
    void deletesOnlyTheSelectedPlayEntryAndUpdatesTheParentRelationship() {
        Game game = gameService.create(game("Game"));
        PlayEntry deleted = playEntryService.create(game.getId(), playEntry("Deleted"));
        PlayEntry retained = playEntryService.create(game.getId(), playEntry("Retained"));

        playEntryService.delete(game.getId(), deleted.getId());

        assertThat(game.getPlayEntries()).containsExactly(retained);
        entityManager.flush();
        entityManager.clear();
        assertThatThrownBy(() -> playEntryService.get(game.getId(), deleted.getId()))
                .isInstanceOf(PlayEntryNotFoundException.class);
        assertThat(playEntryService.get(game.getId(), retained.getId()).getPlaythroughRating())
                .isEqualTo("Retained rating");
    }

    @Test
    void rejectsUnknownGameAndPlayEntryIds() {
        UUID unknownGameId = UUID.randomUUID();
        UUID unknownPlayEntryId = UUID.randomUUID();
        Game game = gameService.create(game("Game"));

        assertThatThrownBy(() -> playEntryService.create(unknownGameId, playEntry("New")))
                .isInstanceOf(GameNotFoundException.class);
        assertThatThrownBy(() -> playEntryService.list(unknownGameId)).isInstanceOf(GameNotFoundException.class);
        assertThatThrownBy(() -> playEntryService.get(unknownGameId, unknownPlayEntryId))
                .isInstanceOf(GameNotFoundException.class);
        assertThatThrownBy(() -> playEntryService.get(game.getId(), unknownPlayEntryId))
                .isInstanceOf(PlayEntryNotFoundException.class);
        assertThatThrownBy(() -> playEntryService.update(game.getId(), unknownPlayEntryId, playEntry("New")))
                .isInstanceOf(PlayEntryNotFoundException.class);
        assertThatThrownBy(() -> playEntryService.delete(game.getId(), unknownPlayEntryId))
                .isInstanceOf(PlayEntryNotFoundException.class);
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
            Path path = java.nio.file.Files.createTempFile("better-game-tracker-play-entries-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not create temporary SQLite database", exception);
        }
    }
}

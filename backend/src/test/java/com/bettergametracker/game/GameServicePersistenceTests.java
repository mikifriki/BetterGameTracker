package com.bettergametracker.game;

import java.nio.file.Path;
import java.util.UUID;

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
class GameServicePersistenceTests {

    private static final Path DATABASE_PATH = createTemporaryDatabasePath();

    @Autowired
    private GameService gameService;

    @Autowired
    private EntityManager entityManager;

    @DynamicPropertySource
    static void temporarySqliteDatabase(DynamicPropertyRegistry registry) {
        registry.add("BETTER_GAME_TRACKER_DATABASE_PATH", DATABASE_PATH::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
    }

    @Test
    void createsAndLoadsAGameWithAllScalarFields() {
        Game game = game("Original game");

        Game created = gameService.create(game);
        entityManager.flush();
        entityManager.clear();

        Game loaded = gameService.get(created.getId());
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getGameTitle()).isEqualTo("Original game");
        assertThat(loaded.getDescription()).isEqualTo("Description for Original game");
        assertThat(loaded.getReleasePlatform()).isEqualTo("PC");
        assertThat(loaded.getReleaseDate()).isEqualTo(java.time.LocalDate.parse("2026-09-07"));
        assertThat(loaded.getDeveloper()).isEqualTo("Developer");
        assertThat(loaded.getMetaRating()).isEqualByComparingTo("9");
        assertThat(loaded.getUserRating()).isEqualByComparingTo("9.0");
        assertThat(loaded.getPhysicalCopy()).isTrue();
    }

    @Test
    void listsAllGames() {
        Game first = gameService.create(game("First"));
        Game second = gameService.create(game("Second"));

        assertThat(gameService.list()).extracting(Game::getId).contains(first.getId(), second.getId());
    }

    @Test
    void updatesScalarFieldsWithoutReplacingTheIdentity() {
        UUID id = gameService.create(game("Original")).getId();

        Game updated = gameService.update(id, game("Replacement"));
        entityManager.flush();
        entityManager.clear();

        Game loaded = gameService.get(id);
        assertThat(updated.getId()).isEqualTo(id);
        assertThat(loaded.getId()).isEqualTo(id);
        assertThat(loaded.getGameTitle()).isEqualTo("Replacement");
        assertThat(loaded.getDescription()).isEqualTo("Description for Replacement");
    }

    @Test
    void deletesOnlyTheSelectedGame() {
        UUID deletedId = gameService.create(game("Deleted")).getId();
        UUID retainedId = gameService.create(game("Retained")).getId();

        gameService.delete(deletedId);
        entityManager.flush();

        assertThatThrownBy(() -> gameService.get(deletedId)).isInstanceOf(GameNotFoundException.class);
        assertThat(gameService.get(retainedId).getGameTitle()).isEqualTo("Retained");
    }

    @Test
    void rejectsUnknownIdsForGetUpdateAndDelete() {
        UUID unknownId = UUID.randomUUID();

        assertThatThrownBy(() -> gameService.get(unknownId)).isInstanceOf(GameNotFoundException.class);
        assertThatThrownBy(() -> gameService.update(unknownId, game("Replacement")))
                .isInstanceOf(GameNotFoundException.class);
        assertThatThrownBy(() -> gameService.delete(unknownId)).isInstanceOf(GameNotFoundException.class);
    }

    private static Game game(String title) {
        Game game = new Game(title);
        game.setDescription("Description for " + title);
        game.setReleasePlatform("PC");
        game.setReleaseDate(java.time.LocalDate.parse("2026-09-07"));
        game.setDeveloper("Developer");
        game.setMetaRating(new java.math.BigDecimal("9"));
        game.setUserRating(new java.math.BigDecimal("9.0"));
        game.setPhysicalCopy(true);
        return game;
    }

    private static Path createTemporaryDatabasePath() {
        try {
            Path path = java.nio.file.Files.createTempFile("better-game-tracker-games-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not create temporary SQLite database", exception);
        }
    }
}

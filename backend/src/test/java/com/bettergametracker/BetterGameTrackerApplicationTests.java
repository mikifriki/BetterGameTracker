package com.bettergametracker;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;

import com.bettergametracker.play.PlayTimeEntry;
import com.bettergametracker.play.PlayTimeEntryRepository;
import java.util.HexFormat;

import com.bettergametracker.game.Game;
import com.bettergametracker.play.PlayEntry;
import com.bettergametracker.review.Review;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class BetterGameTrackerApplicationTests {

    private static final Path DATABASE_PATH = createTemporaryDatabasePath();

    @DynamicPropertySource
    static void temporarySqliteDatabase(DynamicPropertyRegistry registry) {
        registry.add("BETTER_GAME_TRACKER_DATABASE_PATH", DATABASE_PATH::toString);
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + DATABASE_PATH);
    }

    @Test
    void flywayMigratesAndHibernateValidatesTheTemporarySqliteSchema(@Autowired JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                INSERT INTO games (id, game_title)
                VALUES (?, 'V1 game')
                """, bytes("11111111111111111111111111111111"));
        jdbcTemplate.update("""
                INSERT INTO play_entries (id, playthrough_rating, completion_date, platform_played_on,
                    completion_status, location, game_id)
                VALUES (?, '9', '2026-09-06', 'PC', 'COMPLETE', 'Home', ?)
                """, bytes("12121212121212121212121212121212"), bytes("11111111111111111111111111111111"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT time_to_beat_minutes FROM play_entries WHERE id = ?", String.class,
                bytes("12121212121212121212121212121212"))).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT start_date FROM play_entries WHERE id = ?", String.class,
                bytes("12121212121212121212121212121212"))).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT game_title FROM games WHERE id = ?", String.class,
                bytes("11111111111111111111111111111111"))).isEqualTo("V1 game");
        assertThat(jdbcTemplate.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE type = 'SQL' AND success = 1", Integer.class))
                .isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name IN "
                        + "('application_users', 'games', 'play_entries', 'reviews', 'play_time_entries')",
                Integer.class)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT type FROM pragma_table_info('games') WHERE name = 'description'", String.class))
                .isEqualTo("VARCHAR(1000)");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT \"notnull\" FROM pragma_table_info('games') WHERE name = 'game_title'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT \"notnull\" FROM pragma_table_info('games') WHERE name = 'description'", Integer.class))
                .isZero();
    }

    @Test
    void foreignKeysProtectExistingRelationshipsAndUnrelatedGames(@Autowired JdbcTemplate jdbcTemplate) {
        String firstGameId = "01010101010101010101010101010101";
        String unrelatedGameId = "02020202020202020202020202020202";
        String playEntryId = "03030303030303030303030303030303";
        String reviewId = "04040404040404040404040404040404";

        insertGame(jdbcTemplate, firstGameId, "Linked game");
        insertGame(jdbcTemplate, unrelatedGameId, "Unrelated game");
        jdbcTemplate.update("""
                INSERT INTO play_entries (id, playthrough_rating, completion_date, platform_played_on,
                    time_to_beat_minutes, completion_status, location, game_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, bytes(playEntryId), "9", "2026-09-06", "PC", 1200, "COMPLETE", "Home",
                bytes(firstGameId));
        jdbcTemplate.update("""
                INSERT INTO reviews (id, review_date, review_title, review_text, rating, play_entry_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """, bytes(reviewId), "2026-09-06", "Great", "A review", "9", bytes(playEntryId));

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM games WHERE id = ?", bytes(firstGameId)))
                .hasCauseInstanceOf(SQLException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE play_entries SET game_id = ? WHERE id = ?", bytes("05050505050505050505050505050505"),
                bytes(playEntryId)))
                .hasCauseInstanceOf(SQLException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM games WHERE id = ?", Integer.class,
                bytes(unrelatedGameId)))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviews WHERE id = ?", Integer.class,
                bytes(reviewId)))
                .isEqualTo(1);
    }

    @Test
    @Transactional
    void hibernateRoundTripAndDeletionOnlyAffectTheSelectedGame(@Autowired EntityManager entityManager) {
        Game deletedGame = game("Deleted game");
        PlayEntry deletedPlay = playEntry();
        Review deletedReview = review();
        deletedPlay.addReview(deletedReview);
        deletedGame.addPlayEntry(deletedPlay);
        Game retainedGame = game("Retained game");
        PlayEntry retainedPlay = playEntry();
        retainedPlay.addReview(review());
        retainedGame.addPlayEntry(retainedPlay);

        entityManager.persist(deletedGame);
        entityManager.persist(retainedGame);
        entityManager.flush();
        var deletedGameId = deletedGame.getId();
        var deletedPlayId = deletedPlay.getId();
        var deletedReviewId = deletedReview.getId();
        var retainedGameId = retainedGame.getId();
        var retainedPlayId = retainedPlay.getId();
        retainedGame.setGameTitle("Updated retained game");
        entityManager.flush();
        entityManager.remove(deletedGame);
        entityManager.flush();
        entityManager.clear();

        Game reloadedGame = entityManager.find(Game.class, retainedGameId);
        PlayEntry reloadedPlay = entityManager.find(PlayEntry.class, retainedPlayId);
        assertThat(entityManager.find(Game.class, deletedGameId)).isNull();
        assertThat(entityManager.find(PlayEntry.class, deletedPlayId)).isNull();
        assertThat(entityManager.find(Review.class, deletedReviewId)).isNull();
        assertThat(reloadedGame).isNotNull();
        assertThat(reloadedGame.getGameTitle()).isEqualTo("Updated retained game");
        assertThat(reloadedPlay.getGame().getId()).isEqualTo(retainedGameId);
        assertThat(reloadedPlay.getReviews()).hasSize(1);
    }

    @Test
    @Transactional
    void persistedPlayAndReviewOwnershipCannotBeReassigned(@Autowired EntityManager entityManager) {
        Game originalGame = game("Original game");
        Game replacementGame = game("Replacement game");
        PlayEntry originalPlay = playEntry();
        Review originalReview = review();
        originalPlay.addReview(originalReview);
        originalGame.addPlayEntry(originalPlay);
        PlayEntry replacementPlay = playEntry();
        replacementGame.addPlayEntry(replacementPlay);

        entityManager.persist(originalGame);
        entityManager.persist(replacementGame);
        entityManager.flush();
        var originalGameId = originalGame.getId();
        var replacementGameId = replacementGame.getId();
        var originalPlayId = originalPlay.getId();
        var replacementPlayId = replacementPlay.getId();
        var originalReviewId = originalReview.getId();
        entityManager.clear();

        Game reloadedOriginalGame = entityManager.find(Game.class, originalGameId);
        Game reloadedReplacementGame = entityManager.find(Game.class, replacementGameId);
        PlayEntry reloadedOriginalPlay = entityManager.find(PlayEntry.class, originalPlayId);
        PlayEntry reloadedReplacementPlay = entityManager.find(PlayEntry.class, replacementPlayId);
        Review reloadedReview = entityManager.find(Review.class, originalReviewId);

        assertThatThrownBy(() -> reloadedReplacementGame.addPlayEntry(reloadedOriginalPlay))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> reloadedReplacementPlay.addReview(reloadedReview))
                .isInstanceOf(IllegalStateException.class);

        assertThat(reloadedOriginalPlay.getGame().getId()).isEqualTo(originalGameId);
        assertThat(reloadedReplacementGame.getPlayEntries()).containsExactly(reloadedReplacementPlay);
        assertThat(reloadedReview.getPlayEntry().getId()).isEqualTo(originalPlayId);
        assertThat(reloadedReplacementPlay.getReviews()).isEmpty();
    }

    @Test
    @Transactional
    void persistedSameOwnerInstancesWithMatchingIdsRemainIdempotent(@Autowired EntityManager entityManager) {
        Game originalGame = game("Original game");
        PlayEntry originalPlay = playEntry();
        Review originalReview = review();
        originalPlay.addReview(originalReview);
        originalGame.addPlayEntry(originalPlay);
        entityManager.persist(originalGame);
        entityManager.flush();
        var gameId = originalGame.getId();
        var playId = originalPlay.getId();
        var reviewId = originalReview.getId();
        entityManager.clear();

        PlayEntry reloadedPlay = entityManager.find(PlayEntry.class, playId);
        Review reloadedReview = entityManager.find(Review.class, reviewId);
        Game firstGameInstance = entityManager.find(Game.class, gameId);
        entityManager.detach(firstGameInstance);
        Game secondGameInstance = entityManager.find(Game.class, gameId);
        PlayEntry firstPlayInstance = entityManager.find(PlayEntry.class, playId);
        entityManager.detach(firstPlayInstance);
        PlayEntry secondPlayInstance = entityManager.find(PlayEntry.class, playId);

        reloadedPlay.assignGame(secondGameInstance);
        reloadedReview.assignPlayEntry(secondPlayInstance);

        assertThat(reloadedPlay.getGame().getId()).isEqualTo(gameId);
        assertThat(reloadedReview.getPlayEntry().getId()).isEqualTo(playId);
    }

    @Test
    @Transactional
    void optionalTimeEntriesRoundTripWithDuplicateDatesAndCorrectOwners(@Autowired EntityManager entityManager) {
        Game game = game("Timed game");
        PlayEntry emptyPlay = playEntry();
        PlayEntry timedPlay = playEntry();
        game.addPlayEntry(emptyPlay);
        game.addPlayEntry(timedPlay);
        PlayTimeEntry first = timeEntry(30, "First session");
        PlayTimeEntry second = timeEntry(45, null);
        timedPlay.addTimeEntry(first);
        timedPlay.addTimeEntry(second);
        entityManager.persist(game);
        entityManager.flush();
        var firstId = first.getId();
        var secondId = second.getId();
        entityManager.clear();

        assertThat(entityManager.find(PlayEntry.class, emptyPlay.getId()).getTimeEntries()).isEmpty();
        PlayEntry reloaded = entityManager.find(PlayEntry.class, timedPlay.getId());
        assertThat(reloaded.getTimeEntries()).hasSize(2);
        PlayTimeEntry reloadedFirst = entityManager.find(PlayTimeEntry.class, firstId);
        PlayTimeEntry reloadedSecond = entityManager.find(PlayTimeEntry.class, secondId);
        assertThat(firstId).isNotNull().isNotEqualTo(secondId);
        assertThat(reloadedFirst.getPlayEntry().getId()).isEqualTo(timedPlay.getId());
        assertThat(reloadedSecond.getPlayEntry().getId()).isEqualTo(timedPlay.getId());
        assertThat(reloadedFirst.getDate()).isEqualTo(LocalDate.of(2026, 9, 6));
        assertThat(reloadedSecond.getDate()).isEqualTo(reloadedFirst.getDate());
        assertThat(reloadedFirst.getDurationMinutes()).isEqualTo(30);
        assertThat(reloadedSecond.getDurationMinutes()).isEqualTo(45);
        assertThat(reloadedFirst.getNotes()).isEqualTo("First session");
        assertThat(reloadedSecond.getNotes()).isNull();
        reloaded.removeTimeEntry(reloadedFirst);
        entityManager.flush();
        entityManager.clear();
        assertThat(entityManager.find(PlayTimeEntry.class, firstId)).isNull();
        assertThat(entityManager.find(PlayTimeEntry.class, secondId)).isNotNull();
    }

    @Test
    @Transactional
    void timeEntryDatabaseConstraintsProtectDurationAndOwnership(
            @Autowired EntityManager entityManager, @Autowired JdbcTemplate jdbcTemplate) {
        Game game = game("Constraints");
        PlayEntry play = playEntry();
        play.addTimeEntry(timeEntry(1, null));
        game.addPlayEntry(play);
        entityManager.persist(game);
        entityManager.flush();

        for (int duration : new int[] {0, -1}) {
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "UPDATE play_time_entries SET duration_minutes = ?", duration))
                    .hasCauseInstanceOf(SQLException.class);
        }
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE play_time_entries SET play_entry_id = ?", new byte[16]))
                .hasCauseInstanceOf(SQLException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE play_time_entries SET play_entry_id = NULL"))
                .hasCauseInstanceOf(SQLException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE play_time_entries SET date = NULL"))
                .hasCauseInstanceOf(SQLException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM play_entries"))
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    @Transactional
    void timeEntryRepositoryFiltersByPlayEntryAndPreservesDuplicateDates(
            @Autowired EntityManager entityManager, @Autowired PlayTimeEntryRepository repository) {
        Game game = game("Repository game");
        PlayEntry firstPlay = playEntry();
        PlayEntry otherPlay = playEntry();
        game.addPlayEntry(firstPlay);
        game.addPlayEntry(otherPlay);
        entityManager.persist(game);

        PlayTimeEntry first = timeEntry(30, "First session");
        PlayTimeEntry second = timeEntry(45, null);
        PlayTimeEntry other = timeEntry(60, "Other play");
        firstPlay.addTimeEntry(first);
        firstPlay.addTimeEntry(second);
        otherPlay.addTimeEntry(other);
        repository.saveAllAndFlush(java.util.List.of(first, second, other));
        entityManager.clear();

        assertThat(repository.findAllByPlayEntry_Id(firstPlay.getId()))
                .extracting(PlayTimeEntry::getId).containsExactlyInAnyOrder(first.getId(), second.getId());
        assertThat(repository.findAllByPlayEntry_Id(otherPlay.getId()))
                .extracting(PlayTimeEntry::getId).containsExactly(other.getId());
        assertThat(repository.findAllByPlayEntry_Id(java.util.UUID.randomUUID())).isEmpty();
    }

    private static PlayTimeEntry timeEntry(int minutes, String notes) {
        PlayTimeEntry entry = new PlayTimeEntry();
        entry.setDate(LocalDate.of(2026, 9, 6));
        entry.setDurationMinutes(minutes);
        entry.setNotes(notes);
        return entry;
    }

    private static void insertGame(JdbcTemplate jdbcTemplate, String hexadecimalId, String title) {
        jdbcTemplate.update("""
                INSERT INTO games (id, game_title, description, release_platform, release_date, developer,
                    meta_rating, user_rating, physical_copy)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, bytes(hexadecimalId), title, "Description", "PC", "2026-09-06", "Developer", "9", "9", true);
    }

    private static byte[] bytes(String hexadecimalId) {
        return HexFormat.of().parseHex(hexadecimalId);
    }

    private static Game game(String title) {
        Game game = new Game(title);
        game.setDescription("Description");
        game.setReleasePlatform("PC");
        game.setReleaseDate(java.time.LocalDate.parse("2026-09-06"));
        game.setDeveloper("Developer");
        game.setMetaRating(new java.math.BigDecimal("9"));
        game.setUserRating(new java.math.BigDecimal("9"));
        game.setPhysicalCopy(true);
        return game;
    }

    private static PlayEntry playEntry() {
        PlayEntry playEntry = new PlayEntry();
        playEntry.setPlaythroughRating(new java.math.BigDecimal("9"));
        playEntry.setCompletionDate(java.time.LocalDate.parse("2026-09-06"));
        playEntry.setPlatformPlayedOn("PC");
        playEntry.setTimeToBeatMinutes(1200);
        playEntry.setCompletionStatus(com.bettergametracker.play.CompletionStatus.COMPLETE);
        playEntry.setLocation("Home");
        return playEntry;
    }

    private static Review review() {
        Review review = new Review();
        review.setReviewTitle("Great");
        return review;
    }

    private static Path createTemporaryDatabasePath() {
        try {
            Path databasePath = java.nio.file.Files.createTempFile("better-game-tracker-schema-", ".db");
            databasePath.toFile().deleteOnExit();
            return databasePath;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not create temporary SQLite database", exception);
        }
    }
}

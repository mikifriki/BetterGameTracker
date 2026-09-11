package com.bettergametracker.review;

import com.bettergametracker.play.PlayEntry;
import com.bettergametracker.play.PlayEntryService;
import com.bettergametracker.play.PlayEntryNotFoundException;

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
class ReviewServicePersistenceTests {

    private static final Path DATABASE_PATH = createTemporaryDatabasePath();

    @Autowired
    private GameService gameService;

    @Autowired
    private PlayEntryService playEntryService;

    @Autowired
    private ReviewService reviewService;

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
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play")).playEntry();
        UUID id = reviewService.create(game.getId(), play.getId(), review("Original")).getId();
        entityManager.flush();
        entityManager.clear();

        Review loaded = reviewService.get(game.getId(), play.getId(), id);
        assertThat(loaded.getReviewDate()).isEqualTo(java.time.LocalDate.parse("2026-09-07"));
        assertThat(loaded.getReviewTitle()).isEqualTo("Original title");
        assertThat(loaded.getReview()).isEqualTo("Original text");
        assertThat(loaded.getRating()).isEqualByComparingTo("6");
        assertThat(loaded.getPlayEntry().getId()).isEqualTo(play.getId());

        Review replacement = review("Updated");
        replacement.assignPlayEntry(new PlayEntry());
        reviewService.update(game.getId(), play.getId(), id, replacement);
        entityManager.flush();
        entityManager.clear();
        loaded = reviewService.get(game.getId(), play.getId(), id);
        assertThat(loaded.getReviewDate()).isEqualTo(java.time.LocalDate.parse("2026-09-08"));
        assertThat(loaded.getReviewTitle()).isEqualTo("Updated title");
        assertThat(loaded.getReview()).isEqualTo("Updated text");
        assertThat(loaded.getRating()).isEqualByComparingTo("8");
        assertThat(loaded.getPlayEntry().getId()).isEqualTo(play.getId());

        reviewService.update(game.getId(), play.getId(), id, new Review());
        entityManager.flush();
        entityManager.clear();
        loaded = reviewService.get(game.getId(), play.getId(), id);
        assertThat(loaded.getReviewDate()).isNull();
        assertThat(loaded.getReviewTitle()).isNull();
        assertThat(loaded.getReview()).isNull();
        assertThat(loaded.getRating()).isNull();
    }

    @Test
    void listsAndDeletesOnlyReviewsOfTheRequestedPlayEntry() {
        Game game = gameService.create(game("Game"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play")).playEntry();
        PlayEntry other = playEntryService.create(game.getId(), playEntry("Other")).playEntry();
        assertThat(reviewService.list(game.getId(), play.getId())).isEmpty();
        Review first = reviewService.create(game.getId(), play.getId(), review("First"));
        Review second = reviewService.create(game.getId(), play.getId(), review("Second"));
        Review unrelated = reviewService.create(game.getId(), other.getId(), review("Other"));
        entityManager.flush();
        entityManager.clear();
        assertThat(reviewService.list(game.getId(), play.getId())).extracting(Review::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());

        PlayEntry parent = playEntryService.get(game.getId(), play.getId());
        reviewService.delete(game.getId(), play.getId(), first.getId());
        assertThat(parent.getReviews()).extracting(Review::getId).containsExactly(second.getId());
        entityManager.flush();
        entityManager.clear();
        assertThatThrownBy(() -> reviewService.get(game.getId(), play.getId(), first.getId()))
                .isInstanceOf(ReviewNotFoundException.class);
        assertThat(reviewService.list(game.getId(), play.getId())).extracting(Review::getId)
                .containsExactly(second.getId());
        assertThat(reviewService.get(game.getId(), other.getId(), unrelated.getId()).getReview())
                .isEqualTo("Other text");
    }

    @Test
    void rejectsWrongOrMissingParentsForEveryOperation() {
        Game game = gameService.create(game("Game"));
        Game other = gameService.create(game("Other"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play")).playEntry();
        UUID id = reviewService.create(game.getId(), play.getId(), review("Original")).getId();
        entityManager.flush();
        entityManager.clear();

        assertParentRejected(other.getId(), play.getId(), id, PlayEntryNotFoundException.class);
        assertParentRejected(game.getId(), UUID.randomUUID(), id, PlayEntryNotFoundException.class);
        assertParentRejected(UUID.randomUUID(), play.getId(), id, GameNotFoundException.class);
        entityManager.flush();
        entityManager.clear();
        assertThat(reviewService.get(game.getId(), play.getId(), id).getReview()).isEqualTo("Original text");
    }

    @Test
    void rejectsWrongPlayEntryAndMissingReviewsForGetUpdateAndDelete() {
        Game game = gameService.create(game("Game"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play")).playEntry();
        PlayEntry other = playEntryService.create(game.getId(), playEntry("Other")).playEntry();
        UUID id = reviewService.create(game.getId(), play.getId(), review("Original")).getId();
        entityManager.flush();
        entityManager.clear();

        assertReviewRejected(game.getId(), other.getId(), id);
        assertReviewRejected(game.getId(), play.getId(), UUID.randomUUID());
        entityManager.flush();
        entityManager.clear();
        assertThat(reviewService.get(game.getId(), play.getId(), id).getReview()).isEqualTo("Original text");
    }

    @Test
    void rejectsCreatingAReviewAlreadyOwnedByAnotherPlayEntry() {
        Game game = gameService.create(game("Game"));
        PlayEntry play = playEntryService.create(game.getId(), playEntry("Play")).playEntry();
        PlayEntry other = playEntryService.create(game.getId(), playEntry("Other")).playEntry();
        Review review = reviewService.create(game.getId(), play.getId(), review("Original"));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> reviewService.create(game.getId(), other.getId(), review))
                .isInstanceOf(IllegalStateException.class);
        assertThat(reviewService.list(game.getId(), other.getId())).isEmpty();
        assertThat(reviewService.get(game.getId(), play.getId(), review.getId()).getReview())
                .isEqualTo("Original text");
    }

    private void assertParentRejected(UUID gameId, UUID playId, UUID reviewId,
                                      Class<? extends Throwable> exception) {
        assertThatThrownBy(() -> reviewService.create(gameId, playId, review("New"))).isInstanceOf(exception);
        assertThatThrownBy(() -> reviewService.list(gameId, playId)).isInstanceOf(exception);
        assertThatThrownBy(() -> reviewService.get(gameId, playId, reviewId)).isInstanceOf(exception);
        assertThatThrownBy(() -> reviewService.update(gameId, playId, reviewId, review("New")))
                .isInstanceOf(exception);
        assertThatThrownBy(() -> reviewService.delete(gameId, playId, reviewId)).isInstanceOf(exception);
    }

    private void assertReviewRejected(UUID gameId, UUID playId, UUID reviewId) {
        assertThatThrownBy(() -> reviewService.get(gameId, playId, reviewId))
                .isInstanceOf(ReviewNotFoundException.class);
        assertThatThrownBy(() -> reviewService.update(gameId, playId, reviewId, review("New")))
                .isInstanceOf(ReviewNotFoundException.class);
        assertThatThrownBy(() -> reviewService.delete(gameId, playId, reviewId))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    private static Review review(String prefix) {
        Review review = new Review();
        review.setReviewDate(java.time.LocalDate.parse(prefix.equals("Original") ? "2026-09-07" : "2026-09-08"));
        review.setReviewTitle(prefix + " title");
        review.setReview(prefix + " text");
        review.setRating(new java.math.BigDecimal(prefix.equals("Original") ? "6" : "8"));
        return review;
    }

    private static Game game(String title) {
        Game game = new Game(title);
        game.setDescription("Description");
        game.setReleasePlatform("PC");
        game.setReleaseDate(java.time.LocalDate.parse("2026-09-07"));
        game.setDeveloper("Developer");
        game.setMetaRating(new java.math.BigDecimal("9"));
        game.setUserRating(new java.math.BigDecimal("9.0"));
        game.setPhysicalCopy(true);
        return game;
    }

    private static PlayEntry playEntry(String prefix) {
        PlayEntry playEntry = new PlayEntry();
        playEntry.setPlaythroughRating(new java.math.BigDecimal(prefix.equals("Original") ? "6" : "8"));
        playEntry.setCompletionDate(java.time.LocalDate.parse(prefix.equals("Original") ? "2026-09-07" : "2026-09-08"));
        playEntry.setPlatformPlayedOn(prefix + " platform");
        playEntry.setTimeToBeatMinutes(prefix.equals("Original") ? 120 : 180);
        playEntry.setCompletionStatus(com.bettergametracker.play.CompletionStatus.COMPLETE);
        playEntry.setCoop(true);
        playEntry.setLocation(prefix + " location");
        return playEntry;
    }

    private static Path createTemporaryDatabasePath() {
        try {
            Path path = java.nio.file.Files.createTempFile("better-game-tracker-reviews-", ".db");
            path.toFile().deleteOnExit();
            return path;
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not create temporary SQLite database", exception);
        }
    }
}

package com.bettergametracker.domain;

import com.bettergametracker.game.Game;
import com.bettergametracker.play.PlayEntry;
import com.bettergametracker.review.Review;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainRelationshipTests {

    @Test
    void addingPlayEntriesMaintainsBothSidesOfTheGameRelationship() {
        Game game = new Game("Persona 5 Royal");
        PlayEntry firstPlay = new PlayEntry();
        PlayEntry secondPlay = new PlayEntry();

        game.addPlayEntry(firstPlay);
        game.addPlayEntry(secondPlay);

        assertThat(game.getPlayEntries()).containsExactly(firstPlay, secondPlay);
        assertThat(firstPlay.getGame()).isSameAs(game);
        assertThat(secondPlay.getGame()).isSameAs(game);
    }

    @Test
    void addingReviewsMaintainsBothSidesOfThePlayEntryRelationship() {
        PlayEntry playEntry = new PlayEntry();
        Review firstReview = new Review();
        Review secondReview = new Review();

        playEntry.addReview(firstReview);
        playEntry.addReview(secondReview);

        assertThat(playEntry.getReviews()).containsExactly(firstReview, secondReview);
        assertThat(firstReview.getPlayEntry()).isSameAs(playEntry);
        assertThat(secondReview.getPlayEntry()).isSameAs(playEntry);
    }

    @Test
    void reassigningAPlayEntryToAnotherGameIsRejectedWithoutChangingEitherSide() {
        Game originalGame = new Game("Original");
        Game replacementGame = new Game("Replacement");
        PlayEntry playEntry = new PlayEntry();
        originalGame.addPlayEntry(playEntry);

        assertThatThrownBy(() -> replacementGame.addPlayEntry(playEntry))
                .isInstanceOf(IllegalStateException.class);

        assertThat(originalGame.getPlayEntries()).containsExactly(playEntry);
        assertThat(replacementGame.getPlayEntries()).isEmpty();
        assertThat(playEntry.getGame()).isSameAs(originalGame);
    }

    @Test
    void removingThenReattachingAPlayEntryToAnotherGameIsRejected() {
        Game originalGame = new Game("Original");
        Game replacementGame = new Game("Replacement");
        PlayEntry playEntry = new PlayEntry();
        originalGame.addPlayEntry(playEntry);

        originalGame.removePlayEntry(playEntry);

        assertThatThrownBy(() -> replacementGame.addPlayEntry(playEntry))
                .isInstanceOf(IllegalStateException.class);

        assertThat(originalGame.getPlayEntries()).isEmpty();
        assertThat(replacementGame.getPlayEntries()).isEmpty();
        assertThat(playEntry.getGame()).isNull();
    }

    @Test
    void reassigningAReviewToAnotherPlayEntryIsRejectedWithoutChangingEitherSide() {
        PlayEntry originalPlay = new PlayEntry();
        PlayEntry replacementPlay = new PlayEntry();
        Review review = new Review();
        originalPlay.addReview(review);

        assertThatThrownBy(() -> replacementPlay.addReview(review))
                .isInstanceOf(IllegalStateException.class);

        assertThat(originalPlay.getReviews()).containsExactly(review);
        assertThat(replacementPlay.getReviews()).isEmpty();
        assertThat(review.getPlayEntry()).isSameAs(originalPlay);
    }

    @Test
    void removingThenReattachingAReviewToAnotherPlayEntryIsRejected() {
        PlayEntry originalPlay = new PlayEntry();
        PlayEntry replacementPlay = new PlayEntry();
        Review review = new Review();
        originalPlay.addReview(review);

        originalPlay.removeReview(review);

        assertThatThrownBy(() -> replacementPlay.addReview(review))
                .isInstanceOf(IllegalStateException.class);

        assertThat(originalPlay.getReviews()).isEmpty();
        assertThat(replacementPlay.getReviews()).isEmpty();
        assertThat(review.getPlayEntry()).isNull();
    }

    @Test
    void removingChildrenClearsBothSidesOfTheirRelationships() {
        Game game = new Game("Game");
        PlayEntry playEntry = new PlayEntry();
        Review review = new Review();
        game.addPlayEntry(playEntry);
        playEntry.addReview(review);

        playEntry.removeReview(review);
        game.removePlayEntry(playEntry);

        assertThat(review.getPlayEntry()).isNull();
        assertThat(playEntry.getReviews()).isEmpty();
        assertThat(playEntry.getGame()).isNull();
        assertThat(game.getPlayEntries()).isEmpty();
    }
}

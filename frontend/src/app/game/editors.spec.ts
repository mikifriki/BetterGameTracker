import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { GameEditor } from '../library/game-editor';
import { ReviewEditor } from '../playthrough/review-editor';
import { PlayEditor } from './play-editor';

describe('editor validation', () => {
  let http: HttpTestingController;
  beforeAll(() => Object.defineProperties(HTMLDialogElement.prototype, {
    showModal: { configurable: true, value: () => {} },
    close: { configurable: true, value: () => {} }
  }));
  afterAll(() => {
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'showModal');
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'close');
  });
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('rejects blank titles and invalid ratings before saving a game', () => {
    const fixture = TestBed.createComponent(GameEditor);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ gameTitle: '   ' });
    fixture.componentInstance.save();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    fixture.componentInstance.form.patchValue({ gameTitle: 'Game', userRating: 9.99 });
    fixture.componentInstance.save();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    fixture.componentInstance.form.patchValue({ userRating: 9.9 });
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    const request = http.expectOne('/api/v1/games');
    request.flush({ id: 'game', ...request.request.body });
  });

  it('rejects fractional and overflowing manual durations', () => {
    const fixture = TestBed.createComponent(PlayEditor);
    fixture.componentRef.setInput('gameId', 'game');
    fixture.detectChanges();
    for (const duration of [{ hours: 0.5, minutes: 0 }, { hours: 35791394, minutes: 8 }]) {
      fixture.componentInstance.form.patchValue(duration);
      fixture.componentInstance.save();
      expect(fixture.componentInstance.form.invalid).toBe(true);
    }
    fixture.componentInstance.form.patchValue({ hours: 1, minutes: 0 });
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    const request = http.expectOne('/api/v1/games/game/plays');
    request.flush({ id: 'play', gameId: 'game', calculatedTimeMinutes: 0, ...request.request.body });
  });

  it('validates review ratings and prevents duplicate saves', () => {
    const fixture = TestBed.createComponent(ReviewEditor);
    fixture.componentRef.setInput('gameId', 'game');
    fixture.componentRef.setInput('playId', 'play');
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ rating: 1.23 });
    fixture.componentInstance.save();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    fixture.componentInstance.form.patchValue({ rating: 1.2 });
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    const request = http.expectOne('/api/v1/games/game/plays/play/reviews');
    request.flush({ id: 'review', playId: 'play', ...request.request.body });
  });
});

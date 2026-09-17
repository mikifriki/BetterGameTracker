import { ApiService } from '../core/api.service';
import { toLibraryGame } from '../core/formatters';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { PlaythroughDetailsPage } from './playthrough-details-page';

describe('playthrough action errors', () => {
  let fixture: ComponentFixture<PlaythroughDetailsPage>;
  let http: HttpTestingController;
  const time = { id: 'time', playId: 'play', date: '2026-09-15', durationMinutes: 60, notes: null };
  const review = { id: 'review', playId: 'play', reviewDate: null, reviewTitle: 'My review', review: 'Good game', rating: null };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [PlaythroughDetailsPage], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ gameId: 'game', playId: 'play' })), snapshot: { paramMap: convertToParamMap({ gameId: 'game', playId: 'play' }) } } }] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PlaythroughDetailsPage);
    fixture.detectChanges();
    http.expectOne('/api/v1/games/game').flush({ id: 'game', gameTitle: 'Test game' });
    http.expectOne('/api/v1/games/game/plays/play').flush({ id: 'play', gameId: 'game', calculatedTimeMinutes: 60 });
    http.expectOne('/api/v1/games/game/plays/play/time-entries').flush([time]);
    http.expectOne('/api/v1/games/game/plays/play/reviews').flush([review]);
    fixture.detectChanges();
    vi.stubGlobal('confirm', () => true);
  });
  afterEach(() => { http.verify(); vi.unstubAllGlobals(); });

  it('updates the journal and total after deletion without any read requests', () => {
    const api = TestBed.inject(ApiService);
    api.library.set([toLibraryGame({ ...fixture.componentInstance.game()!, plays: [fixture.componentInstance.play()!] })]);
    expect(api.stats().totalMinutes).toBe(60);
    fixture.componentInstance.deleteTime(time);
    http.expectOne('/api/v1/games/game/plays/play/time-entries/time').flush(null);
    expect(fixture.componentInstance.times()).toEqual([]);
    expect(fixture.componentInstance.play()?.calculatedTimeMinutes).toBe(0);
    expect(api.stats().totalMinutes).toBe(0);
    fixture.componentInstance.deleteReview(review);
    http.expectOne('/api/v1/games/game/plays/play/reviews/review').flush(null);
    expect(fixture.componentInstance.reviews()).toEqual([]);
  });

  it('uses saved journal records without fetching unrelated data', () => {
    fixture.componentInstance.timeSaved({ ...time, durationMinutes: 90 });
    fixture.componentInstance.reviewSaved({ ...review, reviewTitle: 'Updated' });
    expect(fixture.componentInstance.times()).toHaveLength(1);
    expect(fixture.componentInstance.play()?.calculatedTimeMinutes).toBe(90);
    expect(fixture.componentInstance.reviews()).toHaveLength(1);
    expect(fixture.componentInstance.reviews()[0].reviewTitle).toBe('Updated');
  });

  it.each(['time', 'review'])('keeps the journal and controls available after a failed %s deletion', kind => {
    const element: HTMLElement = fixture.nativeElement;
    if (kind === 'time') fixture.componentInstance.deleteTime(time);
    else fixture.componentInstance.deleteReview(review);
    const path = `/api/v1/games/game/plays/play/${kind === 'time' ? 'time-entries/time' : 'reviews/review'}`;
    http.expectOne(path).flush({ detail: 'Please retry' }, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();
    expect(element.querySelector('h1')?.textContent).toBe('Playthrough details');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Please retry');
    expect(element.querySelector('.journal-table')?.textContent).toContain('2026-09-15');
    expect(element.querySelector('.review-entry')?.textContent).toContain('My review');
    if (kind === 'time') fixture.componentInstance.deleteTime(time);
    else fixture.componentInstance.deleteReview(review);
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).toBeNull();
    http.expectOne(path).flush({ detail: 'Please retry' }, { status: 503, statusText: 'Service Unavailable' });
  });
});

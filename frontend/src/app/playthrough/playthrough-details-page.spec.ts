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
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ gameId: 'game', playId: 'play' }) } } }] });
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

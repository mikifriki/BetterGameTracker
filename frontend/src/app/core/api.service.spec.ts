import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiService } from './api.service';
import { toLibraryGame } from './formatters';

describe('API logout', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
    api.session.set({ hosted: true, authenticated: true, csrfHeader: 'X-CSRF-TOKEN', csrfToken: 'token' });
    api.library.set([toLibraryGame({ id: 'game', gameTitle: 'Game', description: null, releasePlatform: null,
      releaseDate: null, developer: null, metaRating: null, userRating: null, physicalCopy: null, plays: [] })]);
  });

  afterEach(() => http.verify());

  it('accepts the redirected HTML response and clears the signed-in state', () => {
    const next = vi.fn();
    api.signOut().subscribe(next);
    const request = http.expectOne('/logout');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('token');
    expect(request.request.responseType).toBe('text');
    request.flush('<!doctype html><html></html>');
    expect(next).toHaveBeenCalledOnce();
    expect(api.session()?.authenticated).toBe(false);
    expect(api.session()?.csrfToken).toBeNull();
    expect(api.library()).toEqual([]);
  });

  it('preserves the session and library if logout fails', () => {
    const error = vi.fn();
    api.signOut().subscribe({ error });
    http.expectOne('/logout').flush('Unavailable', { status: 503, statusText: 'Service Unavailable' });
    expect(error).toHaveBeenCalledOnce();
    expect(api.session()?.authenticated).toBe(true);
    expect(api.library()).toHaveLength(1);
  });

  it('keeps CSRF protection and JSON responses for API writes', () => {
    api.deleteGame('game').subscribe();
    const request = http.expectOne('/api/v1/games/game');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('token');
    expect(request.request.responseType).toBe('json');
    request.flush(null);
  });
});

describe('library request reuse', () => {
  let api: ApiService;
  let http: HttpTestingController;
  const game = { id: 'game', gameTitle: 'Game', description: null, releasePlatform: null,
    releaseDate: null, developer: null, metaRating: null, userRating: null, physicalCopy: null };
  const play = { id: 'play', gameId: 'game', completionStatus: 'IN_PROGRESS' as const,
    calculatedTimeMinutes: 60, completionDate: null };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('does not fetch an already loaded empty library again', () => {
    api.loadLibrary().subscribe();
    http.expectOne('/api/v1/games').flush([]);
    api.loadLibrary().subscribe(games => expect(games).toEqual([]));
  });

  it('reuses the initial game and play requests when opening details', () => {
    api.loadLibrary().subscribe();
    http.expectOne('/api/v1/games').flush([game]);
    http.expectOne('/api/v1/games/game/plays').flush([play]);
    api.getGame('game').subscribe(value => expect(value.gameTitle).toBe('Game'));
    api.listPlays('game').subscribe(value => expect(value).toEqual([play]));
    api.getPlay('game', 'play').subscribe(value => expect(value).toEqual(play));
    api.loadLibrary().subscribe();
  });

  it('updates cached records and statistics from write responses without reloads', () => {
    api.loadLibrary().subscribe();
    http.expectOne('/api/v1/games').flush([game]);
    http.expectOne('/api/v1/games/game/plays').flush([play]);
    api.updateGame('game', { ...game, gameTitle: 'Renamed' }).subscribe();
    http.expectOne('/api/v1/games/game').flush({ ...game, gameTitle: 'Renamed' });
    expect(api.library()[0].gameTitle).toBe('Renamed');
    expect(api.stats().totalMinutes).toBe(60);
    api.deletePlay('game', 'play').subscribe();
    http.expectOne('/api/v1/games/game/plays/play').flush(null);
    expect(api.stats().totalMinutes).toBe(0);
    expect(api.stats().backlog).toBe(1);
    api.deleteGame('game').subscribe();
    http.expectOne('/api/v1/games/game').flush(null);
    api.loadLibrary().subscribe(games => expect(games).toEqual([]));
  });

  it('allows retry after a failed library load', () => {
    api.loadLibrary().subscribe({ error: () => {} });
    http.expectOne('/api/v1/games').flush({}, { status: 503, statusText: 'Unavailable' });
    api.loadLibrary().subscribe();
    http.expectOne('/api/v1/games').flush([]);
  });
});

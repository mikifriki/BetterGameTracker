import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GameDetailsPage } from './game-details-page';

describe('game action errors', () => {
  let fixture: ComponentFixture<GameDetailsPage>;
  let http: HttpTestingController;
  let element: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [GameDetailsPage], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ gameId: 'game' }), queryParamMap: convertToParamMap({}) } } }] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(GameDetailsPage);
    element = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne('/api/v1/games/game').flush({ id: 'game', gameTitle: 'Test game' });
    http.expectOne('/api/v1/games/game/plays').flush([]);
    fixture.detectChanges();
    vi.stubGlobal('confirm', () => true);
  });
  afterEach(() => { http.verify(); vi.unstubAllGlobals(); });

  it('keeps the game visible after an upload failure and allows the same file to be retried', () => {
    const input = element.querySelector<HTMLInputElement>('input[type="file"]')!;
    const file = new File(['cover'], 'cover.png', { type: 'image/png' });
    Object.defineProperty(input, 'files', { value: [file] });
    input.dispatchEvent(new Event('change'));
    http.expectOne('/api/v1/games/game/cover').flush({ detail: 'Cover too large' }, { status: 413, statusText: 'Content Too Large' });
    fixture.detectChanges();
    expect(element.querySelector('h1')?.textContent).toBe('Test game');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Cover too large');
    expect(element.querySelector('input[type="file"]')).toBe(input);
    expect(input.value).toBe('');
    input.dispatchEvent(new Event('change'));
    const request = http.expectOne('/api/v1/games/game/cover');
    expect(request.request.body.get('file')).toBe(file);
    request.flush(null);
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).toBeNull();
    expect(element.querySelector('.detail-cover img')?.getAttribute('src')).toContain('v=1');
  });

  it.each(['game', 'play', 'cover'])('keeps controls visible after a failed %s deletion', kind => {
    let path: string;
    if (kind === 'game') {
      fixture.componentInstance.deleteGame();
      path = '/api/v1/games/game';
    } else if (kind === 'play') {
      fixture.componentInstance.deletePlay({ id: 'play' } as Parameters<GameDetailsPage['deletePlay']>[0]);
      path = '/api/v1/games/game/plays/play';
    } else {
      fixture.componentInstance.removeCover();
      path = '/api/v1/games/game/cover';
    }
    http.expectOne(path).flush({ detail: 'Please retry' }, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();
    expect(element.querySelector('h1')?.textContent).toBe('Test game');
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Please retry');
    expect(element.querySelector('input[type="file"]')).not.toBeNull();
  });
});

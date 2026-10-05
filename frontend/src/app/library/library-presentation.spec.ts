import { Component } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterOutlet } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiService } from '../core/api.service';
import { LibraryGame, LibraryStatus } from '../core/models';
import { CollectionStatsBar } from '../layout/collection-stats-bar';
import { LibraryNavigation } from '../layout/library-navigation';
import { LibraryPage } from './library-page';

@Component({
  imports: [RouterOutlet, LibraryNavigation, CollectionStatsBar],
  template: '<bgt-library-navigation /><router-outlet /><bgt-collection-stats-bar />'
})
class LibraryHost {}

const games: LibraryGame[] = (['playing', 'completed', 'backlog', 'dropped'] as LibraryStatus[]).map((status, index) => ({
  id: String(index), gameTitle: `Game ${index}`, status, plays: [], totalMinutes: index * 60,
  userRating: index === 0 ? null : index === 1 ? 0 : 9,
  description: null, releasePlatform: null, releaseDate: null, developer: null, metaRating: null, physicalCopy: null
}));

describe('library presentation', () => {
  let fixture: ComponentFixture<LibraryHost>;
  let element: HTMLElement;
  let router: Router;

  beforeEach(async () => {
    const storage = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value)
    });
    await TestBed.configureTestingModule({
      imports: [LibraryHost],
      providers: [provideHttpClient(), provideRouter([{ path: 'library', component: LibraryPage }])]
    }).compileComponents();
    TestBed.inject(ApiService).library.set(games);
    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(LibraryHost);
    element = fixture.nativeElement;
    fixture.detectChanges();
    await router.navigateByUrl('/library');
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => vi.unstubAllGlobals());

  it('keeps every status readable without its decorative icon and distinguishes zero from an absent rating', () => {
    expect(Array.from(element.querySelectorAll('.game-card .tracking-status'), badge => badge.textContent?.trim()))
      .toEqual(['Playing', 'Completed', 'Backlog', 'Dropped']);
    expect(element.querySelector('.game-card')?.textContent).toContain('Not rated');
    expect(element.querySelectorAll('.game-card')[1].textContent).toContain('0/10');
    expect(element.querySelector('.game-card')?.textContent).toContain('No time logged');
    for (const icon of element.querySelectorAll('bgt-icon')) expect(icon.getAttribute('aria-hidden')).toBe('true');
  });

  it('preserves statuses and ratings when switching to the accessible table view', async () => {
    element.querySelector<HTMLButtonElement>('[aria-label="List view"]')!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelector('[aria-label="List view"]')?.getAttribute('aria-pressed')).toBe('true');
    expect(element.querySelector('.game-grid')).toBeNull();
    expect(element.querySelectorAll('.game-list tbody tr')).toHaveLength(4);
    expect(element.querySelector('.game-list thead')?.textContent).toContain('Personal rating');
    expect(element.querySelectorAll('.game-list .tracking-status')).toHaveLength(4);
    expect(localStorage.getItem('bgt-view')).toBe('list');
  });

  it('updates filter selection and results while the collection strip keeps its full totals', async () => {
    await router.navigateByUrl('/library?status=playing');
    await fixture.whenStable();
    fixture.detectChanges();
    expect(element.querySelectorAll('.game-card')).toHaveLength(1);
    expect(element.querySelector('.library-sidebar [aria-current="page"]')?.textContent?.trim()).toBe('Playing');
    expect(element.querySelectorAll<HTMLSelectElement>('select')[1].value).toBe('playing');
    expect(Array.from(element.querySelectorAll('.stats-strip dd'), value => value.textContent))
      .toEqual(['4', '1', '1', '1', '1', '6h']);
  });

  it('keeps a game link and placeholder when cover loading fails', () => {
    const image = element.querySelector<HTMLImageElement>('.cover-frame img')!;
    image.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(image.hidden).toBe(true);
    expect(element.querySelector('.cover-placeholder')?.textContent).toContain('Game 0');
    expect(element.querySelector('.game-primary-link')?.textContent).toContain('Game 0');
  });

  it('exposes the mobile menu expansion state', async () => {
    const toggle = element.querySelector<HTMLButtonElement>('.mobile-menu-button')!;
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    toggle.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(element.querySelector('#library-menu')?.classList.contains('mobile-open')).toBe(true);
  });
});

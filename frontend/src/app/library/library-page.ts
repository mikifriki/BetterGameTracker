import { PortalIcon } from '../shared/portal-icon';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { ApiService } from '../core/api.service';
import { formatDuration, libraryStatusLabels } from '../core/formatters';
import { Game, LibraryGame, LibraryStatus } from '../core/models';
import { GameEditor } from './game-editor';
import { LibrarySort, LibraryToolbar, ViewMode } from './library-toolbar';

@Component({
  selector: 'bgt-library-page',
  imports: [PortalIcon, RouterLink, LibraryToolbar, GameEditor],
  template: `
    <section class="section-heading page-heading">
      <h1 class="library-title"><bgt-icon name="folder" />My Game Library</h1>
      <span class="result-count" aria-live="polite">{{ games().length }} {{ games().length === 1 ? 'game' : 'games' }}</span>
    </section>

    <bgt-library-toolbar [search]="search()" [sort]="sort()" [status]="status()" [view]="view()"
      (searchChange)="setSearch($event)" (sortChange)="setQuery('sort', $event)" (statusChange)="setQuery('status', $event)" (viewChange)="setView($event)" />

    @if (loading()) {
      <div class="content-state" aria-busy="true"><h2>Loading library…</h2><p>Loading your games and playthroughs.</p></div>
    } @else if (error()) {
      <div class="content-state error" role="alert"><h2>Library unavailable</h2><p>{{ error() }}</p><button class="primary" type="button" (click)="load()">Retry</button></div>
    } @else if (!api.library().length) {
      <div class="content-state"><h2>Your library is ready for its first game.</h2><p>Add a game, then track playthroughs, reviews, and time.</p><button class="primary" type="button" (click)="editing.set(true)">Add Game</button></div>
    } @else if (!games().length) {
      <div class="content-state"><h2>No games found.</h2><p>Try another title or clear your filters.</p><button class="secondary" type="button" (click)="clearFilters()">Clear filters</button></div>
    } @else if (view() === 'grid') {
      <ul class="game-grid" aria-label="Games">
        @for (game of games(); track game.id) {
          <li class="game-card">
            <a class="game-primary-link" [routerLink]="['/games', game.id]" [queryParams]="returnParams()">
              <span class="cover-frame">
                <img [src]="'/api/v1/games/' + game.id + '/cover'" alt="" loading="lazy" (error)="$any($event.target).hidden = true">
                <span class="cover-placeholder" aria-hidden="true"><strong>BGT</strong><small>{{ game.gameTitle }}</small></span>
              </span>
              <span class="game-title">{{ game.gameTitle }}</span>
            </a>
            <span class="tracking-status" [attr.data-status]="game.status"><bgt-icon [name]="game.status" />{{ statusLabel(game.status) }}</span>
            <span class="game-tracking">{{ game.userRating == null ? 'Not rated' : game.userRating + '/10' }} <span aria-hidden="true">·</span> {{ duration(game.totalMinutes) }}</span>
          </li>
        }
      </ul>
    } @else {
      <div class="game-list-wrap">
        <table class="game-list">
          <thead><tr><th>Game</th><th>Status</th><th>Personal rating</th><th>Total time</th></tr></thead>
          <tbody>
            @for (game of games(); track game.id) {
              <tr>
                <th><a [routerLink]="['/games', game.id]" [queryParams]="returnParams()"><img [src]="'/api/v1/games/' + game.id + '/cover'" alt="" (error)="$any($event.target).hidden = true">{{ game.gameTitle }}</a></th>
                <td data-label="Status"><span class="tracking-status" [attr.data-status]="game.status"><bgt-icon [name]="game.status" />{{ statusLabel(game.status) }}</span></td>
                <td data-label="Personal rating">{{ game.userRating == null ? 'Not rated' : game.userRating + '/10' }}</td>
                <td data-label="Total time">{{ duration(game.totalMinutes) }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }

    @if (editing()) { <bgt-game-editor (saved)="gameSaved()" (closed)="closeEditor()" /> }
  `
})
export class LibraryPage implements OnInit {
  readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searches = new Subject<string>();
  readonly loading = signal(false);
  readonly error = signal('');
  readonly search = signal('');
  readonly status = signal<LibraryStatus | ''>('');
  readonly sort = signal<LibrarySort>('title-asc');
  readonly view = signal<ViewMode>((localStorage.getItem('bgt-view') as ViewMode) || 'grid');
  readonly editing = signal(false);
  readonly games = computed(() => this.filteredGames(this.api.library()));
  readonly duration = formatDuration;
  readonly statusLabel = (status: LibraryStatus) => libraryStatusLabels[status];

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.search.set(params.get('q') || '');
      this.status.set((params.get('status') as LibraryStatus) || '');
      this.sort.set((params.get('sort') as LibrarySort) || 'title-asc');
      this.editing.set(params.has('add'));
    });
    this.searches.pipe(debounceTime(250), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(value => this.setQuery('q', value));
    if (!this.api.library().length) this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.loadLibrary().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loading.set(false),
      error: error => { this.loading.set(false); this.error.set(this.api.errorMessage(error)); }
    });
  }

  setSearch(value: string): void { this.search.set(value); this.searches.next(value); }
  setView(value: ViewMode): void { this.view.set(value); localStorage.setItem('bgt-view', value); }
  setQuery(key: string, value: string): void { void this.router.navigate([], { relativeTo: this.route, queryParams: { [key]: value || null }, queryParamsHandling: 'merge' }); }
  clearFilters(): void { void this.router.navigate([], { relativeTo: this.route, queryParams: { q: null, status: null } }); }
  closeEditor(): void { this.editing.set(false); this.setQuery('add', ''); }
  gameSaved(): void { this.closeEditor(); this.load(); }
  returnParams(): Record<string, string> { return { return: this.router.url }; }

  private filteredGames(games: LibraryGame[]): LibraryGame[] {
    const query = this.search().trim().toLocaleLowerCase();
    const filtered = games.filter(game => (!query || game.gameTitle.toLocaleLowerCase().includes(query)) && (!this.status() || game.status === this.status()));
    return filtered.sort((a, b) => {
      if (this.sort() === 'rating-desc') return (b.userRating ?? -1) - (a.userRating ?? -1) || a.gameTitle.localeCompare(b.gameTitle);
      if (this.sort() === 'time-desc') return b.totalMinutes - a.totalMinutes || a.gameTitle.localeCompare(b.gameTitle);
      const direction = this.sort() === 'title-desc' ? -1 : 1;
      return direction * a.gameTitle.localeCompare(b.gameTitle, undefined, { sensitivity: 'base' });
    });
  }
}

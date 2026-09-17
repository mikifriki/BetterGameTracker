import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize, forkJoin, switchMap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { completionLabels, formatDuration } from '../core/formatters';
import { Game, PlayEntry } from '../core/models';
import { GameEditor } from '../library/game-editor';
import { PlayEditor } from './play-editor';

@Component({
  selector: 'bgt-game-details-page',
  imports: [RouterLink, GameEditor, PlayEditor],
  template: `
    @if (loading()) {
      <div class="content-state" aria-busy="true"><h1>Loading game…</h1></div>
    } @else if (error()) {
      <div class="content-state error" role="alert"><h1>Game unavailable</h1><p>{{ error() }}</p><a class="button secondary" routerLink="/library">Return to Library</a></div>
    } @else if (game(); as currentGame) {
      @if (actionError()) { <p class="notice error" role="alert">{{ actionError() }}</p> }
      <nav class="breadcrumbs" aria-label="Breadcrumb"><a [routerLink]="libraryLink()">Library</a><span aria-hidden="true">›</span><span aria-current="page">{{ currentGame.gameTitle }}</span></nav>
      <section class="section-heading page-heading"><div><p class="eyebrow">Game details</p><h1>{{ currentGame.gameTitle }}</h1></div><div class="page-actions"><button type="button" [disabled]="busy()" class="secondary" (click)="editingGame.set(true)">Edit game</button><button type="button" [disabled]="busy()" class="danger-link" (click)="deleteGame()">Delete</button></div></section>

      <section class="game-summary">
        <div class="detail-cover">@if (!coverRemoved()) { <img [src]="coverUrl()" alt="" (load)="$any($event.target).hidden = false" (error)="$any($event.target).hidden = true"> }<span class="cover-placeholder" aria-hidden="true"><strong>BGT</strong><small>{{ currentGame.gameTitle }}</small></span></div>
        <div class="tracker-summary">
          <h2>Tracker summary</h2>
          <dl class="detail-facts">
            <div><dt>Current status</dt><dd>{{ currentStatus() }}</dd></div>
            <div><dt>Personal rating</dt><dd>{{ currentGame.userRating == null ? 'Not rated' : currentGame.userRating + '/10' }}</dd></div>
            <div><dt>Logged time</dt><dd>{{ duration(totalTime()) }}</dd></div>
            <div><dt>Playthroughs</dt><dd>{{ plays().length }}</dd></div>
          </dl>
          <div class="page-actions"><button class="primary" type="button" [disabled]="busy()" (click)="editingPlay.set('new')">Add playthrough</button><label class="button secondary file-button">Upload cover<input type="file" [disabled]="busy()" accept="image/png,image/jpeg" (change)="uploadCover($event)"></label><button class="secondary" type="button" [disabled]="busy()" (click)="removeCover()">Remove cover</button></div>
        </div>
      </section>

      <section class="content-section">
        <div class="section-heading"><div><p class="eyebrow">Tracking history</p><h2>Playthroughs</h2></div><button class="secondary" type="button" [disabled]="busy()" (click)="editingPlay.set('new')">Add playthrough</button></div>
        @if (!plays().length) { <div class="inline-empty"><p>No playthroughs yet. Add one to track status, reviews, and play time.</p></div> }
        @for (play of plays(); track play.id; let index = $index) {
          <article class="play-row">
            <div><h3><a [routerLink]="['/games', currentGame.id, 'playthroughs', play.id]">Playthrough #{{ plays().length - index }}</a></h3><p>{{ play.platformPlayedOn || 'Platform not specified' }} <span aria-hidden="true">·</span> Start: {{ play.startDate || 'Not specified' }} <span aria-hidden="true">·</span> Completion: {{ play.completionDate || 'Not specified' }}</p></div>
            <dl><div><dt>Status</dt><dd>{{ play.completionStatus ? completionLabels[play.completionStatus] : 'Not specified' }}</dd></div><div><dt>Rating</dt><dd>{{ play.playthroughRating == null ? 'Not rated' : play.playthroughRating + '/10' }}</dd></div><div><dt>Logged</dt><dd>{{ duration(play.calculatedTimeMinutes) }}</dd></div></dl>
            <div class="row-actions"><a class="button secondary" [routerLink]="['/games', currentGame.id, 'playthroughs', play.id]">Open log</a><button class="text-button" type="button" [disabled]="busy()" (click)="editingPlay.set(play.id)">Edit</button><button class="danger-link" type="button" [disabled]="busy()" (click)="deletePlay(play)">Delete</button></div>
          </article>
        }
      </section>

      <section class="content-section metadata-section"><div class="section-heading"><div><p class="eyebrow">Catalog details</p><h2>About this game</h2></div></div><p class="description">{{ currentGame.description || 'No description added.' }}</p><dl class="metadata-list"><div><dt>Platform</dt><dd>{{ currentGame.releasePlatform || 'Not specified' }}</dd></div><div><dt>Release date</dt><dd>{{ currentGame.releaseDate || 'Not specified' }}</dd></div><div><dt>Developer</dt><dd>{{ currentGame.developer || 'Not specified' }}</dd></div><div><dt>Critic rating</dt><dd>{{ currentGame.metaRating == null ? 'Not rated' : currentGame.metaRating + '/10' }}</dd></div><div><dt>Format</dt><dd>{{ currentGame.physicalCopy == null ? 'Not specified' : currentGame.physicalCopy ? 'Physical copy' : 'Digital copy' }}</dd></div></dl></section>

      @if (editingGame()) { <bgt-game-editor [game]="currentGame" (saved)="gameSaved($event)" (closed)="editingGame.set(false)" /> }
      @if (editingPlay()) { <bgt-play-editor [gameId]="currentGame.id" [play]="selectedPlay()" (saved)="playSaved($event)" (closed)="editingPlay.set(null)" /> }
    }
  `
})
export class GameDetailsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly api = inject(ApiService);
  readonly game = signal<Game | null>(null);
  readonly plays = signal<PlayEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly busy = signal(false);
  readonly editingGame = signal(false);
  readonly editingPlay = signal<string | 'new' | null>(null);
  readonly completionLabels = completionLabels;
  readonly duration = formatDuration;
  readonly coverVersion = signal(0);
  readonly coverRemoved = signal(false);
  get gameId(): string { return this.route.snapshot.paramMap.get('gameId')!; }

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        const gameId = params.get('gameId')!;
        this.coverRemoved.set(false);
        this.error.set('');
        this.actionError.set('');
        this.loading.set(true);
        this.editingGame.set(false);
        this.editingPlay.set(null);
        return forkJoin({ game: this.api.getGame(gameId), plays: this.api.listPlays(gameId) }).pipe(
          catchError(error => { this.error.set(this.api.errorMessage(error)); this.loading.set(false); return EMPTY; })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => { this.game.set(result.game); this.plays.set(result.plays); this.loading.set(false); });
  }
  libraryLink(): string { const target = this.route.snapshot.queryParamMap.get('return');
    return target === '/library' || target?.startsWith('/library?') ? target : '/library'; }
  coverUrl(): string { return `/api/v1/games/${this.gameId}/cover?v=${this.coverVersion()}`; }
  totalTime(): number { return this.plays().reduce((total, play) => total + play.calculatedTimeMinutes, 0); }
  currentStatus(): string {
    const statuses = this.plays().map(play => play.completionStatus);
    return statuses.includes('IN_PROGRESS') ? 'Playing' : statuses.includes('COMPLETE') ? 'Completed' : statuses.includes('DID_NOT_FINISH') ? 'Dropped' : 'Backlog';
  }
  selectedPlay(): PlayEntry | null { return this.plays().find(play => play.id === this.editingPlay()) || null; }
  gameSaved(game: Game): void { this.game.set(game); this.actionError.set(''); this.editingGame.set(false); }
  playSaved(play: PlayEntry): void {
    this.plays.update(plays => [...plays.filter(existing => existing.id !== play.id), play]
      .sort((a, b) => (b.completionDate || '').localeCompare(a.completionDate || '') || a.id.localeCompare(b.id)));
    this.actionError.set('');
    this.editingPlay.set(null);
  }
  deleteGame(): void {
    if (this.busy()) return;
    if (!confirm(`Delete ${this.game()?.gameTitle}? Its playthroughs, reviews, time entries, and cover will also be deleted.`)) return;
    this.actionError.set('');
    this.busy.set(true);
    this.api.deleteGame(this.gameId).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))
    ).subscribe({
      next: () => { void this.router.navigateByUrl(this.libraryLink()); },
      error: error => this.actionError.set(this.api.errorMessage(error))
    });
  }
  deletePlay(play: PlayEntry): void {
    if (this.busy()) return;
    if (!confirm('Delete this playthrough and all of its reviews and time entries?')) return;
    this.actionError.set('');
    this.busy.set(true);
    this.api.deletePlay(this.gameId, play.id).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))
    ).subscribe({
      next: () => this.plays.update(plays => plays.filter(existing => existing.id !== play.id)),
      error: error => this.actionError.set(this.api.errorMessage(error))
    });
  }
  uploadCover(event: Event): void {
    if (this.busy()) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    input.value = '';
    this.actionError.set('');
    this.busy.set(true);
    this.api.uploadCover(this.gameId, file).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))
    ).subscribe({
      next: () => { this.coverVersion.update(value => value + 1); this.coverRemoved.set(false); },
      error: error => this.actionError.set(this.api.errorMessage(error))
    });
  }
  removeCover(): void {
    if (this.busy()) return;
    this.actionError.set('');
    this.busy.set(true);
    this.api.deleteCover(this.gameId).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))).subscribe({
      next: () => this.coverRemoved.set(true),
      error: error => this.actionError.set(this.api.errorMessage(error))
    });
  }
}

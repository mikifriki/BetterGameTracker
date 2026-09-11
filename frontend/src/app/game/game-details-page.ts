import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
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
      <nav class="breadcrumbs" aria-label="Breadcrumb"><a [href]="libraryLink()">Library</a><span aria-hidden="true">›</span><span aria-current="page">{{ currentGame.gameTitle }}</span></nav>
      <section class="section-heading page-heading"><div><p class="eyebrow">Game details</p><h1>{{ currentGame.gameTitle }}</h1></div><div class="page-actions"><button type="button" class="secondary" (click)="editingGame.set(true)">Edit game</button><button type="button" class="danger-link" (click)="deleteGame()">Delete</button></div></section>

      <section class="game-summary">
        <div class="detail-cover"><img [src]="coverUrl()" alt="" (load)="$any($event.target).hidden = false" (error)="$any($event.target).hidden = true"><span class="cover-placeholder" aria-hidden="true"><strong>BGT</strong><small>{{ currentGame.gameTitle }}</small></span></div>
        <div class="tracker-summary">
          <h2>Tracker summary</h2>
          <dl class="detail-facts">
            <div><dt>Current status</dt><dd>{{ currentStatus() }}</dd></div>
            <div><dt>Personal rating</dt><dd>{{ currentGame.userRating == null ? 'Not rated' : currentGame.userRating + '/10' }}</dd></div>
            <div><dt>Logged time</dt><dd>{{ duration(totalTime()) }}</dd></div>
            <div><dt>Playthroughs</dt><dd>{{ plays().length }}</dd></div>
          </dl>
          <div class="page-actions"><button class="primary" type="button" (click)="editingPlay.set('new')">Add playthrough</button><label class="button secondary file-button">Upload cover<input type="file" accept="image/png,image/jpeg" (change)="uploadCover($event)"></label><button class="secondary" type="button" (click)="removeCover()">Remove cover</button></div>
        </div>
      </section>

      <section class="content-section">
        <div class="section-heading"><div><p class="eyebrow">Tracking history</p><h2>Playthroughs</h2></div><button class="secondary" type="button" (click)="editingPlay.set('new')">Add playthrough</button></div>
        @if (!plays().length) { <div class="inline-empty"><p>No playthroughs yet. Add one to track status, reviews, and play time.</p></div> }
        @for (play of plays(); track play.id; let index = $index) {
          <article class="play-row">
            <div><h3><a [routerLink]="['/games', currentGame.id, 'playthroughs', play.id]">Playthrough #{{ plays().length - index }}</a></h3><p>{{ play.platformPlayedOn || 'Platform not specified' }} <span aria-hidden="true">·</span> {{ play.completionDate || 'No completion date' }}</p></div>
            <dl><div><dt>Status</dt><dd>{{ play.completionStatus ? completionLabels[play.completionStatus] : 'Not specified' }}</dd></div><div><dt>Rating</dt><dd>{{ play.playthroughRating == null ? 'Not rated' : play.playthroughRating + '/10' }}</dd></div><div><dt>Logged</dt><dd>{{ duration(play.calculatedTimeMinutes) }}</dd></div></dl>
            <div class="row-actions"><a class="button secondary" [routerLink]="['/games', currentGame.id, 'playthroughs', play.id]">Open log</a><button class="text-button" type="button" (click)="editingPlay.set(play.id)">Edit</button><button class="danger-link" type="button" (click)="deletePlay(play)">Delete</button></div>
          </article>
        }
      </section>

      <section class="content-section metadata-section"><div class="section-heading"><div><p class="eyebrow">Catalog details</p><h2>About this game</h2></div></div><p class="description">{{ currentGame.description || 'No description added.' }}</p><dl class="metadata-list"><div><dt>Platform</dt><dd>{{ currentGame.releasePlatform || 'Not specified' }}</dd></div><div><dt>Release date</dt><dd>{{ currentGame.releaseDate || 'Not specified' }}</dd></div><div><dt>Developer</dt><dd>{{ currentGame.developer || 'Not specified' }}</dd></div><div><dt>Critic rating</dt><dd>{{ currentGame.metaRating == null ? 'Not rated' : currentGame.metaRating + '/10' }}</dd></div><div><dt>Format</dt><dd>{{ currentGame.physicalCopy == null ? 'Not specified' : currentGame.physicalCopy ? 'Physical copy' : 'Digital copy' }}</dd></div></dl></section>

      @if (editingGame()) { <bgt-game-editor [game]="currentGame" (saved)="saved()" (closed)="editingGame.set(false)" /> }
      @if (editingPlay()) { <bgt-play-editor [gameId]="currentGame.id" [play]="selectedPlay()" (saved)="saved()" (closed)="editingPlay.set(null)" /> }
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
  readonly editingGame = signal(false);
  readonly editingPlay = signal<string | 'new' | null>(null);
  readonly completionLabels = completionLabels;
  readonly duration = formatDuration;
  readonly coverVersion = signal(0);
  readonly gameId = this.route.snapshot.paramMap.get('gameId')!;

  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading.set(true);
    forkJoin({ game: this.api.getGame(this.gameId), plays: this.api.listPlays(this.gameId) }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => { this.game.set(result.game); this.plays.set(result.plays); this.loading.set(false); },
      error: error => { this.error.set(this.api.errorMessage(error)); this.loading.set(false); }
    });
  }
  libraryLink(): string { return this.route.snapshot.queryParamMap.get('return') || '/library'; }
  coverUrl(): string { return `/api/v1/games/${this.gameId}/cover?v=${this.coverVersion()}`; }
  totalTime(): number { return this.plays().reduce((total, play) => total + play.calculatedTimeMinutes, 0); }
  currentStatus(): string {
    const statuses = this.plays().map(play => play.completionStatus);
    return statuses.includes('IN_PROGRESS') ? 'Playing' : statuses.includes('COMPLETE') ? 'Completed' : statuses.includes('DID_NOT_FINISH') ? 'Dropped' : 'Backlog';
  }
  selectedPlay(): PlayEntry | null { return this.plays().find(play => play.id === this.editingPlay()) || null; }
  saved(): void { this.editingGame.set(false); this.editingPlay.set(null); this.load(); this.api.loadLibrary().subscribe(); }
  deleteGame(): void {
    if (!confirm(`Delete ${this.game()?.gameTitle}? Its playthroughs, reviews, time entries, and cover will also be deleted.`)) return;
    this.api.deleteGame(this.gameId).subscribe({ next: () => { this.api.loadLibrary().subscribe(); void this.router.navigateByUrl(this.libraryLink()); }, error: error => this.error.set(this.api.errorMessage(error)) });
  }
  deletePlay(play: PlayEntry): void {
    if (!confirm('Delete this playthrough and all of its reviews and time entries?')) return;
    this.api.deletePlay(this.gameId, play.id).subscribe({ next: () => this.saved(), error: error => this.error.set(this.api.errorMessage(error)) });
  }
  uploadCover(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.[0]) return;
    this.api.uploadCover(this.gameId, input.files[0]).subscribe({ next: () => this.coverVersion.update(value => value + 1), error: error => this.error.set(this.api.errorMessage(error)) });
  }
  removeCover(): void { this.api.deleteCover(this.gameId).subscribe({ next: () => this.coverVersion.update(value => value + 1), error: error => this.error.set(this.api.errorMessage(error)) }); }
}

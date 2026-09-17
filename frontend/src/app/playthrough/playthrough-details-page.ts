import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize, forkJoin, switchMap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { completionLabels, formatDuration } from '../core/formatters';
import { Game, PlayEntry, Review, TimeEntry } from '../core/models';
import { PlayEditor } from '../game/play-editor';
import { ReviewEditor } from './review-editor';
import { TimeEntryEditor } from './time-entry-editor';

@Component({
  selector: 'bgt-playthrough-details-page',
  imports: [RouterLink, PlayEditor, ReviewEditor, TimeEntryEditor],
  template: `
    @if (loading()) { <div class="content-state" aria-busy="true"><h1>Loading playthrough…</h1></div> }
    @else if (error()) { <div class="content-state error" role="alert"><h1>Playthrough unavailable</h1><p>{{ error() }}</p></div> }
    @else if (game(); as currentGame) {
      @if (actionError()) { <p class="notice error" role="alert">{{ actionError() }}</p> }
      <nav class="breadcrumbs" aria-label="Breadcrumb"><a routerLink="/library">Library</a><span aria-hidden="true">›</span><a [routerLink]="['/games', gameId]">{{ currentGame.gameTitle }}</a><span aria-hidden="true">›</span><span aria-current="page">Playthrough</span></nav>
      <section class="section-heading page-heading"><div><p class="eyebrow">{{ currentGame.gameTitle }}</p><h1>Playthrough details</h1></div><button class="secondary" type="button" [disabled]="busy()" (click)="editingPlay.set(true)">Edit playthrough</button></section>
      @if (play(); as currentPlay) {
        <dl class="play-summary detail-facts"><div><dt>Status</dt><dd>{{ currentPlay.completionStatus ? completionLabels[currentPlay.completionStatus] : 'Not specified' }}</dd></div><div><dt>Start date</dt><dd>{{ currentPlay.startDate || 'Not specified' }}</dd></div><div><dt>Completion date</dt><dd>{{ currentPlay.completionDate || 'Not specified' }}</dd></div><div><dt>Personal rating</dt><dd>{{ currentPlay.playthroughRating == null ? 'Not rated' : currentPlay.playthroughRating + '/10' }}</dd></div><div><dt>Logged time</dt><dd>{{ duration(currentPlay.calculatedTimeMinutes) }}</dd></div><div><dt>Platform</dt><dd>{{ currentPlay.platformPlayedOn || 'Not specified' }}</dd></div><div><dt>Location</dt><dd>{{ currentPlay.location || 'Not specified' }}</dd></div></dl>

        <section class="content-section">
          <div class="section-heading"><div><p class="eyebrow">Play journal</p><h2>Time log</h2></div><button class="primary" type="button" [disabled]="busy()" (click)="editingTime.set('new')">Add time entry</button></div>
          @if (!times().length) { <div class="inline-empty"><p>No time logged for this playthrough.</p></div> }
          @else { <div class="journal-table-wrap"><table class="journal-table"><thead><tr><th>Date</th><th>Duration</th><th>Notes</th><th><span class="visually-hidden">Actions</span></th></tr></thead><tbody>@for (entry of times(); track entry.id) { <tr><th>{{ entry.date }}</th><td data-label="Duration">{{ duration(entry.durationMinutes) }}</td><td data-label="Notes" class="notes">{{ entry.notes || '—' }}</td><td class="row-actions"><button class="text-button" type="button" [disabled]="busy()" (click)="editingTime.set(entry.id)">Edit</button><button class="danger-link" type="button" [disabled]="busy()" (click)="deleteTime(entry)">Delete</button></td></tr> }</tbody></table></div> }
        </section>

        <section class="content-section">
          <div class="section-heading"><div><p class="eyebrow">Personal notes</p><h2>Reviews</h2></div><button class="secondary" type="button" [disabled]="busy()" (click)="editingReview.set('new')">Add review</button></div>
          @if (!reviews().length) { <div class="inline-empty"><p>No personal reviews added yet.</p></div> }
          @for (review of reviews(); track review.id) { <article class="review-entry"><div class="review-heading"><div><h3>{{ review.reviewTitle || 'Untitled review' }}</h3><p>{{ review.reviewDate || 'No date' }} <span aria-hidden="true">·</span> {{ review.rating == null ? 'Not rated' : review.rating + '/10' }}</p></div><div class="row-actions"><button class="text-button" type="button" [disabled]="busy()" (click)="editingReview.set(review.id)">Edit</button><button class="danger-link" type="button" [disabled]="busy()" (click)="deleteReview(review)">Delete</button></div></div><p class="review-copy">{{ review.review || 'No review text.' }}</p></article> }
        </section>

        @if (editingPlay()) { <bgt-play-editor [gameId]="gameId" [play]="currentPlay" (saved)="playSaved($event)" (closed)="editingPlay.set(false)" /> }
        @if (editingTime()) { <bgt-time-entry-editor [gameId]="gameId" [playId]="playId" [entry]="selectedTime()" (saved)="timeSaved($event)" (closed)="editingTime.set(null)" /> }
        @if (editingReview()) { <bgt-review-editor [gameId]="gameId" [playId]="playId" [review]="selectedReview()" (saved)="reviewSaved($event)" (closed)="editingReview.set(null)" /> }
      }
    }
  `
})
export class PlaythroughDetailsPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  readonly api = inject(ApiService);
  get gameId(): string { return this.route.snapshot.paramMap.get('gameId')!; }
  get playId(): string { return this.route.snapshot.paramMap.get('playId')!; }
  readonly game = signal<Game | null>(null);
  readonly play = signal<PlayEntry | null>(null);
  readonly times = signal<TimeEntry[]>([]);
  readonly reviews = signal<Review[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly actionError = signal('');
  readonly busy = signal(false);
  readonly editingPlay = signal(false);
  readonly editingTime = signal<string | 'new' | null>(null);
  readonly editingReview = signal<string | 'new' | null>(null);
  readonly completionLabels = completionLabels;
  readonly duration = formatDuration;

  ngOnInit(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        const gameId = params.get('gameId')!;
        const playId = params.get('playId')!;
        this.error.set('');
        this.actionError.set('');
        this.loading.set(true);
        this.editingPlay.set(false);
        this.editingTime.set(null);
        this.editingReview.set(null);
        return forkJoin({ game: this.api.getGame(gameId), play: this.api.getPlay(gameId, playId),
          times: this.api.listTimeEntries(gameId, playId), reviews: this.api.listReviews(gameId, playId) }).pipe(
          catchError(error => { this.error.set(this.api.errorMessage(error)); this.loading.set(false); return EMPTY; })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => {
      this.game.set(result.game); this.play.set(result.play); this.times.set(result.times);
      this.reviews.set(result.reviews); this.loading.set(false);
    });
  }
  playSaved(play: PlayEntry): void { this.play.set(play); this.actionError.set(''); this.editingPlay.set(false); }
  timeSaved(entry: TimeEntry): void {
    this.times.update(entries => [...entries.filter(existing => existing.id !== entry.id), entry]
      .sort((a, b) => b.date.localeCompare(a.date) || a.id.localeCompare(b.id)));
    this.updateLoggedTime();
    this.actionError.set('');
    this.editingTime.set(null);
  }
  reviewSaved(review: Review): void {
    this.reviews.update(reviews => [...reviews.filter(existing => existing.id !== review.id), review]
      .sort((a, b) => (b.reviewDate || '').localeCompare(a.reviewDate || '') || a.id.localeCompare(b.id)));
    this.actionError.set('');
    this.editingReview.set(null);
  }
  private updateLoggedTime(): void {
    const play = { ...this.play()!, calculatedTimeMinutes: this.times().reduce((total, entry) => total + entry.durationMinutes, 0) };
    this.play.set(play);
    this.api.rememberPlay(play);
  }
  selectedTime(): TimeEntry | null { return this.times().find(entry => entry.id === this.editingTime()) || null; }
  selectedReview(): Review | null { return this.reviews().find(review => review.id === this.editingReview()) || null; }
  deleteTime(entry: TimeEntry): void {
    if (this.busy()) return;
    if (!confirm(`Delete the ${entry.date} time entry?`)) return;
    this.actionError.set('');
    this.busy.set(true);
    this.api.deleteTimeEntry(this.gameId, this.playId, entry.id).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))
    ).subscribe({
      next: () => { this.times.update(entries => entries.filter(existing => existing.id !== entry.id)); this.updateLoggedTime(); },
      error: error => this.actionError.set(this.api.errorMessage(error))
    });
  }
  deleteReview(review: Review): void {
    if (this.busy()) return;
    if (!confirm(`Delete ${review.reviewTitle || 'this review'}?`)) return;
    this.actionError.set('');
    this.busy.set(true);
    this.api.deleteReview(this.gameId, this.playId, review.id).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.busy.set(false))
    ).subscribe({
      next: () => this.reviews.update(reviews => reviews.filter(existing => existing.id !== review.id)),
      error: error => this.actionError.set(this.api.errorMessage(error))
    });
  }
}

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../core/api.service';
import { Game, GameInput } from '../core/models';

@Component({
  selector: 'bgt-game-editor',
  imports: [ReactiveFormsModule],
  template: `
    <dialog #dialog (cancel)="saving() ? $event.preventDefault() : closed.emit()" (close)="closed.emit()" aria-labelledby="game-editor-title">
      <form [formGroup]="form" (ngSubmit)="save()">
        <div class="dialog-heading">
          <div><p class="eyebrow">Library record</p><h2 id="game-editor-title">{{ game() ? 'Edit game' : 'Add game' }}</h2></div>
          <button class="icon-button" type="button" [disabled]="saving()" (click)="dialog.close()" aria-label="Close">×</button>
        </div>
        <div class="form-grid">
          <label class="full">Title <input formControlName="gameTitle" maxlength="255" required></label>
          @if (form.controls.gameTitle.touched && form.controls.gameTitle.invalid) { <p class="field-error full">Enter a game title.</p> }
          <label>Platform <input formControlName="releasePlatform" maxlength="255"></label>
          <label>Release date <input type="date" formControlName="releaseDate"></label>
          <label>Developer <input formControlName="developer" maxlength="255"></label>
          <label>Your rating <span class="hint">0–10</span><input type="number" formControlName="userRating" min="0" max="10" step="0.1"></label>
          <label>Critic rating <span class="hint">0–10</span><input type="number" formControlName="metaRating" min="0" max="10" step="0.1"></label>
          <label>Physical copy
            <select formControlName="physicalCopy"><option value="">Not specified</option><option value="true">Yes</option><option value="false">No</option></select>
          </label>
          <label class="full">Description <textarea formControlName="description" maxlength="1000" rows="5"></textarea></label>
        </div>
        @if (form.touched && (form.controls.metaRating.invalid || form.controls.userRating.invalid)) {
          <p class="form-error" role="alert">Ratings must be between 0 and 10 with at most one decimal place.</p>
        }
        @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
        <div class="dialog-actions"><button type="button" class="secondary" [disabled]="saving()" (click)="dialog.close()">Cancel</button><button class="primary" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save game' }}</button></div>
      </form>
    </dialog>
  `
})
export class GameEditor implements AfterViewInit {
  readonly game = input<Game | null>(null);
  readonly saved = output<Game>();
  readonly closed = output<void>();
  readonly dialogRef = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly saving = signal(false);
  readonly error = signal('');
  readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly form = new FormGroup({
    gameTitle: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(255)] }),
    description: new FormControl<string | null>(null, Validators.maxLength(1000)),
    releasePlatform: new FormControl<string | null>(null, Validators.maxLength(255)),
    releaseDate: new FormControl<string | null>(null),
    developer: new FormControl<string | null>(null, Validators.maxLength(255)),
    metaRating: new FormControl<number | null>(null, [Validators.min(0), Validators.max(10), Validators.pattern(/^\d+(\.\d)?$/)]),
    userRating: new FormControl<number | null>(null, [Validators.min(0), Validators.max(10), Validators.pattern(/^\d+(\.\d)?$/)]),
    physicalCopy: new FormControl('')
  });

  ngAfterViewInit(): void {
    const game = this.game();
    if (game) this.form.patchValue({ ...game, physicalCopy: game.physicalCopy == null ? '' : String(game.physicalCopy) });
    this.dialogRef().nativeElement.showModal();
  }

  get dialog(): HTMLDialogElement { return this.dialogRef().nativeElement; }

  save(): void {
    if (this.saving()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const value: GameInput = {
      ...raw,
      gameTitle: raw.gameTitle.trim(),
      description: raw.description || null,
      releasePlatform: raw.releasePlatform || null,
      releaseDate: raw.releaseDate || null,
      developer: raw.developer || null,
      physicalCopy: raw.physicalCopy === '' ? null : raw.physicalCopy === 'true'
    };
    this.error.set('');
    this.saving.set(true);
    const request = this.game() ? this.api.updateGame(this.game()!.id, value) : this.api.createGame(value);
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: game => { this.dialog.close(); this.saved.emit(game); },
      error: error => this.error.set(this.api.errorMessage(error))
    });
  }
}

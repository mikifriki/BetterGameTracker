import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../core/api.service';
import { PlayEntry, PlayInput } from '../core/models';

@Component({
  selector: 'bgt-play-editor',
  imports: [ReactiveFormsModule],
  template: `
    <dialog #dialog (cancel)="saving() ? $event.preventDefault() : closed.emit()" (close)="closed.emit()" aria-labelledby="play-editor-title">
      <form [formGroup]="form" (ngSubmit)="save()">
        <div class="dialog-heading">
          <div><p class="eyebrow">Tracking record</p><h2 id="play-editor-title">{{ play() ? 'Edit playthrough' : 'Add playthrough' }}</h2></div>
          <button class="icon-button" type="button" [disabled]="saving()" (click)="dialog.close()" aria-label="Close">×</button>
        </div>
        <div class="form-grid">
          <label>Status
            <select formControlName="completionStatus"><option value="">Not specified</option><option value="IN_PROGRESS">In progress</option><option value="COMPLETE">Complete</option><option value="DID_NOT_FINISH">Did not finish</option></select>
          </label>
          <label>Personal rating <span class="hint">0–10</span><input type="number" formControlName="playthroughRating" min="0" max="10" step="0.1"></label>
          <label>Start date <input type="date" formControlName="startDate"></label>
          <label>Completion date <input type="date" formControlName="completionDate"></label>
          <label>Platform played on <input formControlName="platformPlayedOn" maxlength="255"></label>
          <fieldset class="duration-fields"><legend>Manual total time <span class="hint">optional</span></legend><label>Hours <input type="number" formControlName="hours" min="0" max="35791394" step="1"></label><label>Minutes <input type="number" formControlName="minutes" min="0" max="59" step="1"></label></fieldset>
          <label>Location <input formControlName="location" maxlength="255"></label>
          <label>Co-op
            <select formControlName="coop"><option value="">Not specified</option><option value="true">Yes</option><option value="false">No</option></select>
          </label>
        </div>
        @if (form.touched && form.invalid) { <p class="form-error" role="alert">Check the rating and duration. Use whole hours and minutes.</p> }
        @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
        <div class="dialog-actions"><button type="button" class="secondary" [disabled]="saving()" (click)="dialog.close()">Cancel</button><button class="primary" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save playthrough' }}</button></div>
      </form>
    </dialog>
  `
})
export class PlayEditor implements AfterViewInit {
  readonly gameId = input.required<string>();
  readonly play = input<PlayEntry | null>(null);
  readonly saved = output<PlayEntry>();
  readonly closed = output<void>();
  readonly dialogRef = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly saving = signal(false);
  readonly error = signal('');
  private readonly defaultStartDate = (() => {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  })();
  readonly form = new FormGroup({
    completionStatus: new FormControl(''),
    playthroughRating: new FormControl<number | null>(null, [Validators.min(0), Validators.max(10), Validators.pattern(/^\d+(\.\d)?$/)]),
    startDate: new FormControl<string | null>(this.defaultStartDate),
    completionDate: new FormControl<string | null>(null),
    platformPlayedOn: new FormControl<string | null>(null, Validators.maxLength(255)),
    hours: new FormControl<number | null>(null, [Validators.min(0), Validators.max(35791394)]),
    minutes: new FormControl<number | null>(null, [Validators.min(0), Validators.max(59)]),
    location: new FormControl<string | null>(null, Validators.maxLength(255)),
    coop: new FormControl('')
  }, { validators: control => {
    const { hours, minutes } = control.value;
    const total = (hours ?? 0) * 60 + (minutes ?? 0);
    return Number.isInteger(hours ?? 0) && Number.isInteger(minutes ?? 0) && total <= 2147483647
      ? null : { duration: true };
  } });

  ngAfterViewInit(): void {
    const play = this.play();
    if (play) this.form.patchValue({
      ...play,
      completionStatus: play.completionStatus || '',
      hours: play.timeToBeatMinutes == null ? null : Math.floor(play.timeToBeatMinutes / 60),
      minutes: play.timeToBeatMinutes == null ? null : play.timeToBeatMinutes % 60,
      coop: play.coop == null ? '' : String(play.coop)
    });
    this.dialogRef().nativeElement.showModal();
  }

  get dialog(): HTMLDialogElement { return this.dialogRef().nativeElement; }

  save(): void {
    if (this.saving()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const hasDuration = raw.hours != null || raw.minutes != null;
    const value: PlayInput = {
      completionStatus: raw.completionStatus ? raw.completionStatus as PlayInput['completionStatus'] : null,
      playthroughRating: raw.playthroughRating,
      startDate: raw.startDate || null,
      completionDate: raw.completionDate || null,
      platformPlayedOn: raw.platformPlayedOn || null,
      timeToBeatMinutes: hasDuration ? (raw.hours || 0) * 60 + (raw.minutes || 0) : null,
      location: raw.location || null,
      coop: raw.coop === '' ? null : raw.coop === 'true'
    };
    this.error.set('');
    this.saving.set(true);
    const play = this.play();
    const request = play ? this.api.updatePlay(this.gameId(), play.id, value) : this.api.createPlay(this.gameId(), value);
    request.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: saved => { this.dialog.close(); this.saved.emit(saved); },
      error: error => this.error.set(this.api.errorMessage(error))
    });
  }
}

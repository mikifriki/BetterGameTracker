import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../core/api.service';
import { TimeEntry, TimeEntryInput } from '../core/models';

@Component({
  selector: 'bgt-time-entry-editor',
  imports: [ReactiveFormsModule],
  template: `
    <dialog #dialog (cancel)="saving() ? $event.preventDefault() : closed.emit()" (close)="closed.emit()" aria-labelledby="time-title">
      <form [formGroup]="form" (ngSubmit)="save()">
        <div class="dialog-heading"><div><p class="eyebrow">Play journal</p><h2 id="time-title">{{ entry() ? 'Edit time entry' : 'Add time entry' }}</h2></div><button class="icon-button" type="button" [disabled]="saving()" (click)="dialog.close()" aria-label="Close">×</button></div>
        <div class="form-grid">
          <label>Date <input type="date" formControlName="date" required></label>
          <fieldset class="duration-fields"><legend>Duration</legend><label>Hours <input type="number" formControlName="hours" min="0" step="1"></label><label>Minutes <input type="number" formControlName="minutes" min="0" max="59" step="1"></label></fieldset>
          <label class="full">Notes <textarea formControlName="notes" maxlength="255" rows="5"></textarea></label>
        </div>
        @if (form.touched && form.invalid) { <p class="form-error" role="alert">Enter a date and a duration greater than zero, using whole hours and minutes (at most 2,147,483,647 minutes).</p> }
        @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
        <div class="dialog-actions"><button type="button" class="secondary" [disabled]="saving()" (click)="dialog.close()">Cancel</button><button class="primary" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save entry' }}</button></div>
      </form>
    </dialog>
  `
})
export class TimeEntryEditor implements AfterViewInit {
  readonly gameId = input.required<string>();
  readonly playId = input.required<string>();
  readonly entry = input<TimeEntry | null>(null);
  readonly saved = output<TimeEntry>();
  readonly closed = output<void>();
  readonly dialogRef = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly saving = signal(false);
  readonly error = signal('');
  private readonly defaultDate = (() => {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  })();
  readonly form = new FormGroup({
    date: new FormControl(this.defaultDate, { nonNullable: true, validators: Validators.required }),
    hours: new FormControl<number>(0, { nonNullable: true, validators: Validators.min(0) }),
    minutes: new FormControl<number>(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(59)] }),
    notes: new FormControl<string | null>(null, Validators.maxLength(255))
  }, { validators: control => {
    const { hours, minutes } = control.value;
    const total = (hours ?? 0) * 60 + (minutes ?? 0);
    return Number.isInteger(hours ?? 0) && Number.isInteger(minutes ?? 0) && total >= 1 && total <= 2147483647
      ? null : { duration: true };
  } });

  ngAfterViewInit(): void {
    const entry = this.entry();
    if (entry) this.form.patchValue({ date: entry.date, hours: Math.floor(entry.durationMinutes / 60), minutes: entry.durationMinutes % 60, notes: entry.notes });
    this.dialogRef().nativeElement.showModal();
  }
  get dialog(): HTMLDialogElement { return this.dialogRef().nativeElement; }
  save(): void {
    if (this.saving()) return;
    const raw = this.form.getRawValue();
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value: TimeEntryInput = { date: raw.date, durationMinutes: raw.hours * 60 + raw.minutes, notes: raw.notes || null };
    this.error.set('');
    this.saving.set(true);
    this.api.saveTimeEntry(this.gameId(), this.playId(), value, this.entry()?.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({
      next: saved => { this.dialog.close(); this.saved.emit(saved); },
      error: error => this.error.set(this.api.errorMessage(error))
    });
  }
}

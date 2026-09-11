import { AfterViewInit, Component, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../core/api.service';
import { TimeEntry, TimeEntryInput } from '../core/models';

@Component({
  selector: 'bgt-time-entry-editor',
  imports: [ReactiveFormsModule],
  template: `
    <dialog #dialog (cancel)="closed.emit()" (close)="closed.emit()" aria-labelledby="time-title">
      <form [formGroup]="form" (ngSubmit)="save()">
        <div class="dialog-heading"><div><p class="eyebrow">Play journal</p><h2 id="time-title">{{ entry() ? 'Edit time entry' : 'Add time entry' }}</h2></div><button class="icon-button" type="button" (click)="dialog.close()" aria-label="Close">×</button></div>
        <div class="form-grid">
          <label>Date <input type="date" formControlName="date" required></label>
          <fieldset class="duration-fields"><legend>Duration</legend><label>Hours <input type="number" formControlName="hours" min="0" step="1"></label><label>Minutes <input type="number" formControlName="minutes" min="0" max="59" step="1"></label></fieldset>
          <label class="full">Notes <textarea formControlName="notes" maxlength="255" rows="5"></textarea></label>
        </div>
        @if (form.touched && form.invalid) { <p class="form-error" role="alert">Enter a date and a duration greater than zero.</p> }
        @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
        <div class="dialog-actions"><button type="button" class="secondary" (click)="dialog.close()">Cancel</button><button class="primary" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save entry' }}</button></div>
      </form>
    </dialog>
  `
})
export class TimeEntryEditor implements AfterViewInit {
  readonly gameId = input.required<string>();
  readonly playId = input.required<string>();
  readonly entry = input<TimeEntry | null>(null);
  readonly saved = output<void>();
  readonly closed = output<void>();
  readonly dialogRef = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly api = inject(ApiService);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = new FormGroup({
    date: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: Validators.required }),
    hours: new FormControl<number>(0, { nonNullable: true, validators: Validators.min(0) }),
    minutes: new FormControl<number>(0, { nonNullable: true, validators: [Validators.min(0), Validators.max(59)] }),
    notes: new FormControl<string | null>(null, Validators.maxLength(255))
  });

  ngAfterViewInit(): void {
    const entry = this.entry();
    if (entry) this.form.patchValue({ date: entry.date, hours: Math.floor(entry.durationMinutes / 60), minutes: entry.durationMinutes % 60, notes: entry.notes });
    this.dialogRef().nativeElement.showModal();
  }
  get dialog(): HTMLDialogElement { return this.dialogRef().nativeElement; }
  save(): void {
    const raw = this.form.getRawValue();
    if (this.form.invalid || raw.hours * 60 + raw.minutes < 1) { this.form.markAllAsTouched(); return; }
    const value: TimeEntryInput = { date: raw.date, durationMinutes: raw.hours * 60 + raw.minutes, notes: raw.notes || null };
    this.saving.set(true);
    this.api.saveTimeEntry(this.gameId(), this.playId(), value, this.entry()?.id).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => { this.saved.emit(); this.dialog.close(); },
      error: error => this.error.set(this.api.errorMessage(error))
    });
  }
}

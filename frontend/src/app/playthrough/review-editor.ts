import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AfterViewInit, Component, DestroyRef, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../core/api.service';
import { Review, ReviewInput } from '../core/models';

@Component({
  selector: 'bgt-review-editor',
  imports: [ReactiveFormsModule],
  template: `
    <dialog #dialog (cancel)="saving() ? $event.preventDefault() : closed.emit()" (close)="closed.emit()" aria-labelledby="review-title">
      <form [formGroup]="form" (ngSubmit)="save()">
        <div class="dialog-heading"><div><p class="eyebrow">Personal review</p><h2 id="review-title">{{ review() ? 'Edit review' : 'Add review' }}</h2></div><button class="icon-button" type="button" [disabled]="saving()" (click)="dialog.close()" aria-label="Close">×</button></div>
        <div class="form-grid"><label>Title <input formControlName="reviewTitle" maxlength="255"></label><label>Date <input type="date" formControlName="reviewDate"></label><label>Rating <span class="hint">0–10</span><input type="number" formControlName="rating" min="0" max="10" step="0.1"></label><label class="full">Review <textarea formControlName="review" maxlength="5000" rows="10"></textarea></label></div>
        @if (form.controls.rating.touched && form.controls.rating.invalid) {
          <p class="form-error" role="alert">Rating must be between 0 and 10 with at most one decimal place.</p>
        }
        @if (error()) { <p class="form-error" role="alert">{{ error() }}</p> }
        <div class="dialog-actions"><button type="button" class="secondary" [disabled]="saving()" (click)="dialog.close()">Cancel</button><button class="primary" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save review' }}</button></div>
      </form>
    </dialog>
  `
})
export class ReviewEditor implements AfterViewInit {
  readonly gameId = input.required<string>();
  readonly playId = input.required<string>();
  readonly review = input<Review | null>(null);
  readonly saved = output<Review>();
  readonly closed = output<void>();
  readonly dialogRef = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  readonly api = inject(ApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = new FormGroup({ reviewTitle: new FormControl<string | null>(null, Validators.maxLength(255)), reviewDate: new FormControl<string | null>(null), rating: new FormControl<number | null>(null, [Validators.min(0), Validators.max(10), Validators.pattern(/^\d+(\.\d)?$/)]), review: new FormControl<string | null>(null, Validators.maxLength(5000)) });

  ngAfterViewInit(): void { if (this.review()) this.form.patchValue(this.review()!); this.dialogRef().nativeElement.showModal(); }
  get dialog(): HTMLDialogElement { return this.dialogRef().nativeElement; }
  save(): void {
    if (this.saving()) return;
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    const value: ReviewInput = { reviewTitle: raw.reviewTitle || null, reviewDate: raw.reviewDate || null, rating: raw.rating, review: raw.review || null };
    this.error.set('');
    this.saving.set(true);
    this.api.saveReview(this.gameId(), this.playId(), value, this.review()?.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.saving.set(false))).subscribe({ next: saved => { this.dialog.close(); this.saved.emit(saved); }, error: error => this.error.set(this.api.errorMessage(error)) });
  }
}

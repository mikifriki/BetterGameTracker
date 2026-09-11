import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { of, switchMap } from 'rxjs';
import { ApiService } from './core/api.service';
import { BrandHeader } from './layout/brand-header';
import { LibraryNavigation } from './layout/library-navigation';
import { CollectionStatsBar } from './layout/collection-stats-bar';
import { SiteFooter } from './layout/site-footer';

@Component({
  selector: 'bgt-root',
  imports: [RouterOutlet, BrandHeader, LibraryNavigation, CollectionStatsBar, SiteFooter],
  templateUrl: './app.html'
})
export class App implements OnInit {
  readonly api = inject(ApiService);
  readonly loading = signal(true);
  readonly error = signal('');
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit(): void {
    this.api.loadSession().pipe(
      switchMap(session => session.hosted && !session.authenticated ? of([]) : this.api.loadLibrary()),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: () => this.loading.set(false),
      error: error => {
        this.loading.set(false);
        this.error.set(this.api.errorMessage(error));
      }
    });
  }
}

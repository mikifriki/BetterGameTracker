import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../core/api.service';

@Component({
  selector: 'bgt-brand-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="site-header">
      <a class="brand" routerLink="/library" aria-label="BetterGameTracker library">
        <span class="brand-name">BetterGameTracker</span>
      </a>
      <svg class="header-controller" viewBox="0 0 180 110" fill="none" stroke="currentColor" stroke-width="3" aria-hidden="true" focusable="false">
        <path d="M48 22c-15-3-22 5-27 21L7 89c-4 17 9 20 20 8l24-23h78l24 23c11 12 24 9 20-8l-14-46c-5-16-12-24-27-21Z" />
        <path d="M46 36v12H34v12h12v12h12V60h12V48H58V36Z" />
        <circle cx="135" cy="41" r="5" /><circle cx="148" cy="54" r="5" /><circle cx="122" cy="54" r="5" /><circle cx="135" cy="67" r="5" />
        <path d="M80 59h8m6 0h8M85 22V9h18" />
      </svg>
      <nav class="global-nav" aria-label="Primary">
        <a routerLink="/library" routerLinkActive="active" ariaCurrentWhenActive="page">Library</a>
        <a routerLink="/stats" routerLinkActive="active" ariaCurrentWhenActive="page">Stats</a>
        <a routerLink="/settings" routerLinkActive="active" ariaCurrentWhenActive="page">Settings</a>
        @if (api.session()?.authenticated) {
          <button type="button" class="nav-button" [disabled]="signingOut()" (click)="signOut()">Sign out</button>
        }
      </nav>
    </header>
    @if (error()) { <p class="notice error" role="alert">{{ error() }}</p> }
  `
})
export class BrandHeader {
  readonly api = inject(ApiService);

  readonly signingOut = signal(false);
  readonly error = signal('');

  signOut(): void {
    if (this.signingOut()) return;
    this.signingOut.set(true);
    this.error.set('');
    this.api.signOut().pipe(finalize(() => this.signingOut.set(false))).subscribe({
      next: () => location.assign('/'),
      error: error => this.error.set(this.api.errorMessage(error))
    });
  }
}

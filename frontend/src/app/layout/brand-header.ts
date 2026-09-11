import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ApiService } from '../core/api.service';

@Component({
  selector: 'bgt-brand-header',
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="site-header">
      <a class="brand" routerLink="/library" aria-label="BetterGameTracker library">
        <span class="brand-mark" aria-hidden="true">BGT</span>
        <span class="brand-name">BetterGameTracker<small>Personal game library</small></span>
      </a>
      <nav class="global-nav" aria-label="Primary">
        <a routerLink="/library" routerLinkActive="active" ariaCurrentWhenActive="page">Library</a>
        <a routerLink="/stats" routerLinkActive="active" ariaCurrentWhenActive="page">Stats</a>
        <a routerLink="/settings" routerLinkActive="active" ariaCurrentWhenActive="page">Settings</a>
        @if (api.session()?.authenticated) {
          <button type="button" class="nav-button" (click)="signOut()">Sign out</button>
        }
      </nav>
    </header>
  `
})
export class BrandHeader {
  readonly api = inject(ApiService);

  signOut(): void {
    this.api.signOut().subscribe(() => location.assign('/'));
  }
}

import { PortalIcon } from '../shared/portal-icon';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'bgt-library-navigation',
  imports: [PortalIcon, RouterLink],
  template: `
    <aside class="library-sidebar">
      <button class="mobile-menu-button" type="button" (click)="open.update(value => !value)" [attr.aria-expanded]="open()" aria-controls="library-menu">
        Library menu <span aria-hidden="true">{{ open() ? '−' : '+' }}</span>
      </button>
      <nav id="library-menu" aria-label="Library" [class.mobile-open]="open()">
        <h2>Main menu</h2>
        <a routerLink="/library" [class.selected]="libraryPage() && !status()" [attr.aria-current]="libraryPage() && !status() ? 'page' : null"><bgt-icon name="folder" /><span>All Games</span></a>
        <a routerLink="/library" [queryParams]="{ status: 'playing' }" [class.selected]="libraryPage() && status() === 'playing'" [attr.aria-current]="libraryPage() && status() === 'playing' ? 'page' : null"><bgt-icon name="playing" /><span>Playing</span></a>
        <a routerLink="/library" [queryParams]="{ status: 'completed' }" [class.selected]="libraryPage() && status() === 'completed'" [attr.aria-current]="libraryPage() && status() === 'completed' ? 'page' : null"><bgt-icon name="completed" /><span>Completed</span></a>
        <a routerLink="/library" [queryParams]="{ status: 'backlog' }" [class.selected]="libraryPage() && status() === 'backlog'" [attr.aria-current]="libraryPage() && status() === 'backlog' ? 'page' : null"><bgt-icon name="backlog" /><span>Backlog</span></a>
        <a routerLink="/library" [queryParams]="{ status: 'dropped' }" [class.selected]="libraryPage() && status() === 'dropped'" [attr.aria-current]="libraryPage() && status() === 'dropped' ? 'page' : null"><bgt-icon name="dropped" /><span>Dropped</span></a>
        <div class="sidebar-action">
          <a routerLink="/library" [queryParams]="{ add: 1 }"><bgt-icon name="add" /><span>Add Game</span></a>
        </div>
      </nav>
    </aside>
  `
})
export class LibraryNavigation implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly open = signal(false);
  readonly status = signal('');
  readonly libraryPage = signal(false);

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => this.status.set(params.get('status') || ''));
    this.libraryPage.set(this.router.url.split('?')[0] === '/library');
    this.router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.libraryPage.set(this.router.url.split('?')[0] === '/library'));
  }
}

import { PortalIcon } from '../shared/portal-icon';
import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LibraryStatus } from '../core/models';

export type LibrarySort = 'title-asc' | 'title-desc' | 'rating-desc' | 'time-desc';
export type ViewMode = 'grid' | 'list';

@Component({
  selector: 'bgt-library-toolbar',
  imports: [PortalIcon, FormsModule],
  template: `
    <div class="library-toolbar" aria-label="Library controls">
      <label class="search-control">Search
        <span class="input-with-action">
          <input type="search" [ngModel]="search()" (ngModelChange)="searchChange.emit($event)" autocomplete="off" placeholder="Game title">
          @if (search()) { <button type="button" (click)="searchChange.emit('')" aria-label="Clear search">×</button> }
        </span>
      </label>
      <label>Sort
        <select [ngModel]="sort()" (ngModelChange)="sortChange.emit($event)">
          <option value="title-asc">Title — A to Z</option>
          <option value="title-desc">Title — Z to A</option>
          <option value="rating-desc">Personal rating — high first</option>
          <option value="time-desc">Total time — high first</option>
        </select>
      </label>
      <label>Status
        <select [ngModel]="status()" (ngModelChange)="statusChange.emit($event)">
          <option value="">All Games</option>
          <option value="playing">Playing</option>
          <option value="completed">Completed</option>
          <option value="backlog">Backlog</option>
          <option value="dropped">Dropped</option>
        </select>
      </label>
      <div class="view-toggle" aria-label="View mode">
        <button type="button" [class.active]="view() === 'grid'" [attr.aria-pressed]="view() === 'grid'" (click)="viewChange.emit('grid')" aria-label="Grid view"><bgt-icon name="grid" /></button>
        <button type="button" [class.active]="view() === 'list'" [attr.aria-pressed]="view() === 'list'" (click)="viewChange.emit('list')" aria-label="List view"><bgt-icon name="list" /></button>
      </div>
    </div>
  `
})
export class LibraryToolbar {
  readonly search = input.required<string>();
  readonly sort = input.required<LibrarySort>();
  readonly status = input<LibraryStatus | ''>('');
  readonly view = input.required<ViewMode>();
  readonly searchChange = output<string>();
  readonly sortChange = output<LibrarySort>();
  readonly statusChange = output<LibraryStatus | ''>();
  readonly viewChange = output<ViewMode>();
}

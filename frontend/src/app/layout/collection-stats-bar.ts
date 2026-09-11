import { Component, computed, inject } from '@angular/core';
import { ApiService } from '../core/api.service';
import { formatDuration } from '../core/formatters';

@Component({
  selector: 'bgt-collection-stats-bar',
  template: `
    <section class="stats-strip" aria-label="Collection statistics">
      <dl>
        <div><dt>Library:</dt><dd>{{ stats().library }}</dd></div>
        <div><dt>Playing:</dt><dd>{{ stats().playing }}</dd></div>
        <div><dt>Completed:</dt><dd>{{ stats().completed }}</dd></div>
        <div><dt>Backlog:</dt><dd>{{ stats().backlog }}</dd></div>
        <div><dt>Dropped:</dt><dd>{{ stats().dropped }}</dd></div>
        <div><dt>Total Time:</dt><dd>{{ duration(stats().totalMinutes) }}</dd></div>
      </dl>
    </section>
  `
})
export class CollectionStatsBar {
  private readonly api = inject(ApiService);
  readonly stats = computed(() => this.api.stats());
  readonly duration = formatDuration;
}

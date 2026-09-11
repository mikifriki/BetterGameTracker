import { Component, computed, inject } from '@angular/core';
import { ApiService } from '../core/api.service';
import { formatDuration } from '../core/formatters';

@Component({
  selector: 'bgt-stats-page',
  template: `
    <section class="section-heading page-heading"><div><p class="eyebrow">Collection overview</p><h1>Stats</h1></div></section>
    <section class="content-section stats-page">
      <h2>Library breakdown</h2>
      <dl class="stats-list"><div><dt>All games</dt><dd>{{ stats().library }}</dd></div><div><dt>Playing</dt><dd>{{ stats().playing }}</dd></div><div><dt>Completed</dt><dd>{{ stats().completed }}</dd></div><div><dt>Backlog</dt><dd>{{ stats().backlog }}</dd></div><div><dt>Dropped</dt><dd>{{ stats().dropped }}</dd></div><div><dt>Total logged time</dt><dd>{{ duration(stats().totalMinutes) }}</dd></div></dl>
      <p class="section-note">Statistics describe your full collection. Logged time is calculated from individual playtime entries.</p>
    </section>
  `
})
export class StatsPage {
  private readonly api = inject(ApiService);
  readonly stats = computed(() => this.api.stats());
  readonly duration = formatDuration;
}

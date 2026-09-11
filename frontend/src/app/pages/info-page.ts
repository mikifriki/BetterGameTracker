import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'bgt-info-page',
  imports: [RouterLink],
  template: `
    <section class="section-heading page-heading"><div><p class="eyebrow">BetterGameTracker</p><h1>{{ help ? 'Help' : 'About' }}</h1></div></section>
    <section class="content-section prose">
      @if (help) { <h2>Track your library</h2><p>Add games from the Library menu. Open a game to record playthroughs, then use each playthrough log for dated time entries and personal reviews.</p><p>Search, status, and sorting controls stay in the URL so you can return to the same library view.</p><a routerLink="/library">Return to Library</a> }
      @else { <h2>A focused personal game tracker</h2><p>BetterGameTracker keeps your games, playthroughs, reviews, cover art, and playing time together in one self-hostable library.</p><p>The interface takes its compact visual direction from late-1990s and early-2000s gaming and sports portals while retaining modern accessibility and responsive behavior.</p> }
    </section>
  `
})
export class InfoPage {
  readonly help = inject(ActivatedRoute).snapshot.data['page'] === 'help';
}

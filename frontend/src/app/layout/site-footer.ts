import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'bgt-site-footer',
  imports: [RouterLink],
  template: `
    <footer class="site-footer">
      <span>BetterGameTracker</span>
      <nav aria-label="Secondary"><a routerLink="/help">Help</a><a routerLink="/about">About</a></nav>
    </footer>
  `
})
export class SiteFooter {}

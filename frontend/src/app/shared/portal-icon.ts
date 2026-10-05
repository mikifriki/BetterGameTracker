import { Component, input } from '@angular/core';

export type PortalIconName = 'folder' | 'playing' | 'completed' | 'backlog' | 'dropped' | 'add' | 'clock' | 'grid' | 'list';

@Component({
  selector: 'bgt-icon',
  host: { 'aria-hidden': 'true' },
  template: `
    <svg viewBox="0 0 24 24" fill="none" stroke="#102e50" stroke-width="1.5" stroke-linejoin="round" aria-hidden="true" focusable="false">
      @switch (name()) {
        @case ('folder') {
          <path d="M2 5h7l2 3h11v13H2Z" fill="#89acc8" />
          <path d="M2 5h7l2 3h10v4H2Z" fill="#eaf5fc" />
          <path d="M2 11h20v10H2Z" fill="#477fae" />
          <path d="M4 13h16" stroke="#bce0f7" />
        }
        @case ('playing') {
          <path d="M5 2 22 12 5 22Z" fill="#2889d8" />
          <path d="m7 5 11 7H7Z" fill="#76c6fc" stroke="none" />
        }
        @case ('completed') {
          <path d="M4 22V3" stroke-width="2.5" />
          <path d="M5 4c5-5 9 5 16 1l-3 6 3 5c-7 4-11-6-16-1Z" fill="#319752" />
          <path d="M7 5c3-1 6 3 10 3" stroke="#a3e9ad" />
        }
        @case ('backlog') {
          <path d="m3 6 5-4h14v16l-5 4H3Z" fill="#9baebd" />
          <path d="m3 6 5-4h14l-5 4Z" fill="#e9f0f5" />
          <path d="M3 6h14v16H3ZM3 11h14M3 16h14M17 6l5-4M17 11l5-4M17 16l5-4" />
          <path d="M5 8h10M5 13h10M5 18h10" stroke="#edf5fa" />
        }
        @case ('dropped') {
          <path d="m6 2 6 6 6-6 4 4-6 6 6 6-4 4-6-6-6 6-4-4 6-6-6-6Z" fill="#be3d49" />
          <path d="m6 5 6 6 6-6" stroke="#f59c9b" />
        }
        @case ('add') {
          <path d="M2 2h20v20H2Z" fill="#2d8448" />
          <path d="M4 20V4h16" stroke="#9dd9a4" />
          <path d="M12 6v12M6 12h12" stroke="#fff" stroke-width="3" />
        }
        @case ('clock') {
          <circle cx="12" cy="12" r="10" fill="#a4c9e4" />
          <circle cx="12" cy="12" r="7.5" fill="#f2f8fc" stroke="none" />
          <path d="M12 6v7l5 3" stroke-width="2" />
        }
        @case ('grid') {
          <path d="M3 3h7v7H3ZM14 3h7v7h-7ZM3 14h7v7H3ZM14 14h7v7h-7Z" fill="currentColor" stroke="currentColor" />
        }
        @case ('list') {
          <path d="M3 4h3v3H3ZM3 11h3v3H3ZM3 18h3v3H3Z" fill="currentColor" stroke="currentColor" />
          <path d="M9 5.5h12M9 12.5h12M9 19.5h12" stroke="currentColor" stroke-width="3" />
        }
      }
    </svg>
  `
})
export class PortalIcon {
  readonly name = input.required<PortalIconName>();
}

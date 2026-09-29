import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'library' },
  { path: 'library', loadComponent: () => import('./library/library-page').then(m => m.LibraryPage) },
  { path: 'games/:gameId', loadComponent: () => import('./game/game-details-page').then(m => m.GameDetailsPage) },
  { path: 'games/:gameId/playthroughs/:playId', loadComponent: () => import('./playthrough/playthrough-details-page').then(m => m.PlaythroughDetailsPage) },
  { path: 'stats', loadComponent: () => import('./pages/stats-page').then(m => m.StatsPage) },
  { path: 'settings', loadComponent: () => import('./pages/settings-page').then(m => m.SettingsPage) },
  { path: 'help', loadComponent: () => import('./pages/info-page').then(m => m.InfoPage), data: { page: 'help' } },
  { path: 'about', loadComponent: () => import('./pages/info-page').then(m => m.InfoPage), data: { page: 'about' } },
  { path: '**', redirectTo: 'library' }
];

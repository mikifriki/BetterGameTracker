import { Component, inject } from '@angular/core';
import { ApiService } from '../core/api.service';

@Component({
  selector: 'bgt-settings-page',
  template: `
    <section class="section-heading page-heading"><div><p class="eyebrow">Application</p><h1>Settings</h1></div></section>
    <section class="content-section settings-page"><h2>Deployment</h2><dl class="metadata-list"><div><dt>Mode</dt><dd>{{ api.session()?.hosted ? 'Hosted' : 'Local' }}</dd></div><div><dt>Account</dt><dd>{{ api.session()?.authenticated ? 'Signed in with Google' : api.session()?.hosted ? 'Not signed in' : 'No account required' }}</dd></div><div><dt>Library view</dt><dd>Grid or list selection is saved in this browser.</dd></div></dl><p class="section-note">Server and database settings are managed by the BetterGameTracker deployment.</p></section>
  `
})
export class SettingsPage { readonly api = inject(ApiService); }

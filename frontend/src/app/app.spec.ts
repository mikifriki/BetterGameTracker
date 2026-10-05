import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './app';
import { LibraryPage } from './library/library-page';

describe('application startup', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {} });
    TestBed.configureTestingModule({ imports: [App], providers: [provideHttpClient(), provideHttpClientTesting(),
      provideRouter([{ path: 'library', component: LibraryPage }])] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); vi.unstubAllGlobals(); });

  it('does not issue library requests after session loading fails and supports retry', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http.expectOne('/api/v1/session').flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeNull();
    fixture.nativeElement.querySelector('main button').click();
    http.expectOne('/api/v1/session').flush({ hosted: true, authenticated: false });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Continue with Google');
  });

  it('loads an empty library only once across startup and route activation', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http.expectOne('/api/v1/session').flush({ hosted: false, authenticated: false });
    http.expectOne('/api/v1/games').flush([]);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/library');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Your library is ready for its first game.');
  });
});

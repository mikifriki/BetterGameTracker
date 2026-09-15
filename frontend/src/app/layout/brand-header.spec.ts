import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { ApiService } from '../core/api.service';
import { BrandHeader } from './brand-header';

describe('sign-out controls', () => {
  it('shows a failure and allows retry without duplicate pending requests', () => {
    TestBed.configureTestingModule({ imports: [BrandHeader],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    TestBed.inject(ApiService).session.set({ hosted: true, authenticated: true, csrfHeader: 'X-CSRF-TOKEN', csrfToken: 'token' });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(BrandHeader);
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const button = element.querySelector<HTMLButtonElement>('button')!;
    button.click();
    fixture.detectChanges();
    expect(button.disabled).toBe(true);
    button.click();
    http.expectOne('/logout').flush('Unavailable', { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();
    expect(button.disabled).toBe(false);
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('503');
    button.click();
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).toBeNull();
    http.expectOne('/logout').flush('Unavailable', { status: 503, statusText: 'Service Unavailable' });
    http.verify();
  });
});

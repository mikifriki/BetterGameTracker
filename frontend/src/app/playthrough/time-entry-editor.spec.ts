import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { TimeEntryEditor } from './time-entry-editor';

describe('time entry editor', () => {
  let http: HttpTestingController;

  // jsdom does not implement the native dialog methods.
  beforeAll(() => Object.defineProperties(HTMLDialogElement.prototype, {
    showModal: { configurable: true, value: () => {} },
    close: { configurable: true, value: () => {} }
  }));
  afterAll(() => {
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'showModal');
    Reflect.deleteProperty(HTMLDialogElement.prototype, 'close');
  });
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [TimeEntryEditor], providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { http.verify(); vi.useRealTimers(); });

  it.each([0, 23])('defaults to the local calendar date at %i:30', hour => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 8, 15, hour, 30));
    const fixture = TestBed.createComponent(TimeEntryEditor);
    fixture.componentRef.setInput('gameId', 'game');
    fixture.componentRef.setInput('playId', 'play');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[type="date"]').value).toBe('2026-09-15');
  });

  it('shows the zero-duration error and saves after a valid correction', () => {
    const fixture = TestBed.createComponent(TimeEntryEditor);
    fixture.componentRef.setInput('gameId', 'game');
    fixture.componentRef.setInput('playId', 'play');
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('duration greater than zero');
    http.expectNone('/api/v1/games/game/plays/play/time-entries');
    const minutes = element.querySelector<HTMLInputElement>('[formControlName="minutes"]')!;
    minutes.value = '1';
    minutes.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).toBeNull();
    element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const request = http.expectOne('/api/v1/games/game/plays/play/time-entries');
    expect(request.request.body.durationMinutes).toBe(1);
    request.flush({ id: 'entry', playId: 'play', ...request.request.body });
  });

  it('prevents duplicate submissions while a save is pending', () => {
    const fixture = TestBed.createComponent(TimeEntryEditor);
    fixture.componentRef.setInput('gameId', 'game');
    fixture.componentRef.setInput('playId', 'play');
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ minutes: 1 });
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    const request = http.expectOne('/api/v1/games/game/plays/play/time-entries');
    request.flush({ id: 'entry', playId: 'play', ...request.request.body });
  });

  it.each([{ hours: 0.5, minutes: 0 }, { hours: 0, minutes: 1.5 }, { hours: 35791394, minutes: 8 }])(
    'rejects invalid duration %o before sending a request', duration => {
      const fixture = TestBed.createComponent(TimeEntryEditor);
      fixture.componentRef.setInput('gameId', 'game');
      fixture.componentRef.setInput('playId', 'play');
      fixture.detectChanges();
      fixture.componentInstance.form.patchValue(duration);
      fixture.componentInstance.save();
      expect(fixture.componentInstance.form.invalid).toBe(true);
    });

  it('preserves an existing date and saves an hours-only duration', () => {
    const fixture = TestBed.createComponent(TimeEntryEditor);
    fixture.componentRef.setInput('gameId', 'game');
    fixture.componentRef.setInput('playId', 'play');
    fixture.componentRef.setInput('entry', { id: 'entry', playId: 'play', date: '2020-01-02', durationMinutes: 60, notes: null });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('input[type="date"]').value).toBe('2020-01-02');
    fixture.componentInstance.save();
    const request = http.expectOne('/api/v1/games/game/plays/play/time-entries/entry');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ date: '2020-01-02', durationMinutes: 60, notes: null });
    request.flush({ id: 'entry', playId: 'play', ...request.request.body });
  });
});

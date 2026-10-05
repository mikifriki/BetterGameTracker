import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { forkJoin, map, Observable, of, switchMap, tap } from 'rxjs';
import { CollectionStats, Game, GameInput, LibraryGame, PlayEntry, PlayInput, Review, ReviewInput, Session, TimeEntry, TimeEntryInput } from './models';
import { toLibraryGame } from './formatters';

@Injectable({ providedIn: 'root' })
export class ApiService {
  readonly session = signal<Session | null>(null);
  readonly library = signal<LibraryGame[]>([]);
  private libraryLoaded = false;

  constructor(private readonly http: HttpClient) {}

  loadSession(): Observable<Session> {
    return this.http.get<Session>('/api/v1/session').pipe(tap(session => this.session.set(session)));
  }

  loadLibrary(): Observable<LibraryGame[]> {
    if (this.libraryLoaded) return of(this.library());
    return this.http.get<Game[]>('/api/v1/games').pipe(
      switchMap(games => games.length
        ? forkJoin(games.map(game => this.listPlays(game.id).pipe(map(plays => toLibraryGame({ ...game, plays })))))
        : of([] as LibraryGame[])),
      tap(games => { this.library.set(games); this.libraryLoaded = true; })
    );
  }

  stats(): CollectionStats {
    const games = this.library();
    return {
      library: games.length,
      playing: games.filter(game => game.status === 'playing').length,
      completed: games.filter(game => game.status === 'completed').length,
      backlog: games.filter(game => game.status === 'backlog').length,
      dropped: games.filter(game => game.status === 'dropped').length,
      totalMinutes: games.reduce((total, game) => total + game.totalMinutes, 0)
    };
  }

  getGame(id: string): Observable<Game> {
    const game = this.library().find(game => game.id === id);
    return game ? of(game) : this.http.get<Game>(`/api/v1/games/${id}`);
  }

  createGame(value: GameInput): Observable<Game> {
    return this.write<Game>('post', '/api/v1/games', value).pipe(
      tap(game => this.library.update(games => [...games, toLibraryGame({ ...game, plays: [] })]))
    );
  }

  updateGame(id: string, value: GameInput): Observable<Game> {
    return this.write<Game>('put', `/api/v1/games/${id}`, value).pipe(
      tap(game => this.library.update(games => games.map(existing => existing.id === id
        ? toLibraryGame({ ...game, plays: existing.plays }) : existing)))
    );
  }

  deleteGame(id: string): Observable<void> {
    return this.write<void>('delete', `/api/v1/games/${id}`).pipe(
      tap(() => this.library.update(games => games.filter(game => game.id !== id)))
    );
  }

  listPlays(gameId: string): Observable<PlayEntry[]> {
    const game = this.library().find(game => game.id === gameId);
    return game ? of(game.plays) : this.http.get<PlayEntry[]>(`/api/v1/games/${gameId}/plays`);
  }

  getPlay(gameId: string, playId: string): Observable<PlayEntry> {
    const play = this.library().find(game => game.id === gameId)?.plays.find(play => play.id === playId);
    return play ? of(play) : this.http.get<PlayEntry>(`/api/v1/games/${gameId}/plays/${playId}`);
  }

  createPlay(gameId: string, value: PlayInput): Observable<PlayEntry> {
    return this.write<PlayEntry>('post', `/api/v1/games/${gameId}/plays`, value).pipe(tap(play => this.rememberPlay(play)));
  }

  updatePlay(gameId: string, playId: string, value: PlayInput): Observable<PlayEntry> {
    return this.write<PlayEntry>('put', `/api/v1/games/${gameId}/plays/${playId}`, value).pipe(tap(play => this.rememberPlay(play)));
  }

  deletePlay(gameId: string, playId: string): Observable<void> {
    return this.write<void>('delete', `/api/v1/games/${gameId}/plays/${playId}`).pipe(
      tap(() => this.updateLibraryPlays(gameId, plays => plays.filter(play => play.id !== playId)))
    );
  }

  rememberPlay(play: PlayEntry): void {
    this.updateLibraryPlays(play.gameId, plays => [...plays.filter(existing => existing.id !== play.id), play]
      .sort((a, b) => (b.completionDate || '').localeCompare(a.completionDate || '') || a.id.localeCompare(b.id)));
  }

  private updateLibraryPlays(gameId: string, update: (plays: PlayEntry[]) => PlayEntry[]): void {
    this.library.update(games => games.map(game => game.id === gameId
      ? toLibraryGame({ ...game, plays: update(game.plays) }) : game));
  }

  listTimeEntries(gameId: string, playId: string): Observable<TimeEntry[]> { return this.http.get<TimeEntry[]>(`/api/v1/games/${gameId}/plays/${playId}/time-entries`); }
  saveTimeEntry(gameId: string, playId: string, value: TimeEntryInput, id?: string): Observable<TimeEntry> {
    const path = `/api/v1/games/${gameId}/plays/${playId}/time-entries${id ? `/${id}` : ''}`;
    return this.write<TimeEntry>(id ? 'put' : 'post', path, value);
  }
  deleteTimeEntry(gameId: string, playId: string, id: string): Observable<void> { return this.write<void>('delete', `/api/v1/games/${gameId}/plays/${playId}/time-entries/${id}`); }

  listReviews(gameId: string, playId: string): Observable<Review[]> { return this.http.get<Review[]>(`/api/v1/games/${gameId}/plays/${playId}/reviews`); }
  saveReview(gameId: string, playId: string, value: ReviewInput, id?: string): Observable<Review> {
    const path = `/api/v1/games/${gameId}/plays/${playId}/reviews${id ? `/${id}` : ''}`;
    return this.write<Review>(id ? 'put' : 'post', path, value);
  }
  deleteReview(gameId: string, playId: string, id: string): Observable<void> { return this.write<void>('delete', `/api/v1/games/${gameId}/plays/${playId}/reviews/${id}`); }

  uploadCover(gameId: string, file: File): Observable<void> {
    const body = new FormData();
    body.append('file', file);
    return this.write<void>('put', `/api/v1/games/${gameId}/cover`, body);
  }
  deleteCover(gameId: string): Observable<void> { return this.write<void>('delete', `/api/v1/games/${gameId}/cover`); }

  signOut(): Observable<string> {
    return this.http.post('/logout', null, { headers: this.csrfHeaders(), responseType: 'text' }).pipe(
      tap(() => {
        this.library.set([]);
        this.libraryLoaded = false;
        this.session.update(session => session ? { ...session, authenticated: false, csrfToken: null } : session);
      })
    );
  }

  errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      return error.error?.detail || error.error?.title || `Request failed (${error.status}).`;
    }
    return 'Something went wrong. Please try again.';
  }

  private csrfHeaders(): HttpHeaders | undefined {
    const session = this.session();
    return session?.csrfToken && session.csrfHeader
      ? new HttpHeaders().set(session.csrfHeader, session.csrfToken)
      : undefined;
  }

  private write<T>(method: 'post' | 'put' | 'delete', path: string, body?: unknown): Observable<T> {
    return this.http.request<T>(method, path, { body, headers: this.csrfHeaders() }).pipe(
      tap({ error: error => { if (error instanceof HttpErrorResponse && error.status === 401) this.session.update(value => value ? { ...value, authenticated: false } : value); } })
    );
  }
}

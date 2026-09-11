export type CompletionStatus = 'IN_PROGRESS' | 'COMPLETE' | 'DID_NOT_FINISH';
export type LibraryStatus = 'playing' | 'completed' | 'backlog' | 'dropped';

export interface Session {
  hosted: boolean;
  authenticated: boolean;
  csrfToken: string | null;
  csrfHeader: string | null;
}

export interface Game {
  id: string;
  gameTitle: string;
  description: string | null;
  releasePlatform: string | null;
  releaseDate: string | null;
  developer: string | null;
  metaRating: number | null;
  userRating: number | null;
  physicalCopy: boolean | null;
}

export interface PlayEntry {
  id: string;
  gameId: string;
  playthroughRating: number | null;
  completionDate: string | null;
  platformPlayedOn: string | null;
  timeToBeatMinutes: number | null;
  completionStatus: CompletionStatus | null;
  coop: boolean | null;
  location: string | null;
  calculatedTimeMinutes: number;
}

export interface TimeEntry {
  id: string;
  playId: string;
  date: string;
  durationMinutes: number;
  notes: string | null;
}

export interface Review {
  id: string;
  playId: string;
  reviewDate: string | null;
  reviewTitle: string | null;
  review: string | null;
  rating: number | null;
}

export interface LibraryGame extends Game {
  plays: PlayEntry[];
  status: LibraryStatus;
  totalMinutes: number;
}

export interface CollectionStats {
  library: number;
  playing: number;
  completed: number;
  backlog: number;
  dropped: number;
  totalMinutes: number;
}

export type GameInput = Omit<Game, 'id'>;
export type PlayInput = Omit<PlayEntry, 'id' | 'gameId' | 'calculatedTimeMinutes'>;
export type TimeEntryInput = Pick<TimeEntry, 'date' | 'durationMinutes' | 'notes'>;
export type ReviewInput = Omit<Review, 'id' | 'playId'>;

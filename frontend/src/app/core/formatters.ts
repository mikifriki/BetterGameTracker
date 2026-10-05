import { CompletionStatus, LibraryGame, LibraryStatus } from './models';

export const completionLabels: Record<CompletionStatus, string> = {
  IN_PROGRESS: 'In progress',
  COMPLETE: 'Complete',
  DID_NOT_FINISH: 'Did not finish'
};

export const libraryStatusLabels: Record<LibraryStatus, string> = {
  playing: 'Playing',
  completed: 'Completed',
  backlog: 'Backlog',
  dropped: 'Dropped'
};

export function formatDuration(minutes: number | null | undefined, empty = 'No time logged'): string {
  if (minutes == null || minutes === 0) return empty;
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return hours ? `${hours.toLocaleString()}h${remainder ? ` ${remainder}m` : ''}` : `${remainder}m`;
}

export function deriveStatus(statuses: (CompletionStatus | null)[]): LibraryStatus {
  if (statuses.includes('IN_PROGRESS')) return 'playing';
  if (statuses.includes('COMPLETE')) return 'completed';
  if (statuses.includes('DID_NOT_FINISH')) return 'dropped';
  return 'backlog';
}

export function toLibraryGame(game: Omit<LibraryGame, 'status' | 'totalMinutes'>): LibraryGame {
  return {
    ...game,
    status: deriveStatus(game.plays.map(play => play.completionStatus)),
    totalMinutes: game.plays.reduce((total, play) => total + play.calculatedTimeMinutes, 0)
  };
}

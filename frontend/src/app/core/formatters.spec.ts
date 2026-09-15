import { describe, expect, it } from 'vitest';
import { deriveStatus, formatDuration, toLibraryGame } from './formatters';
import { Game, PlayEntry } from './models';

describe('library formatters', () => {
  it('formats absent, minute-only, and hour durations', () => {
    expect(formatDuration(null)).toBe('No time logged');
    expect(formatDuration(45)).toBe('45m');
    expect(formatDuration(145)).toBe('2h 25m');
  });

  it('derives one current library status using the documented priority', () => {
    expect(deriveStatus([])).toBe('backlog');
    expect(deriveStatus(['DID_NOT_FINISH'])).toBe('dropped');
    expect(deriveStatus(['DID_NOT_FINISH', 'COMPLETE'])).toBe('completed');
    expect(deriveStatus(['COMPLETE', 'IN_PROGRESS'])).toBe('playing');
  });

  it('totals authoritative logged time without adding the manual total', () => {
    const game: Game = {
      id: 'game', gameTitle: 'Game', description: null, releasePlatform: null,
      releaseDate: null, developer: null, metaRating: null, userRating: null, physicalCopy: null
    };
    const play = (id: string, calculatedTimeMinutes: number, completionStatus: PlayEntry['completionStatus']): PlayEntry => ({
      id, gameId: game.id, calculatedTimeMinutes, completionStatus, playthroughRating: null,
      startDate: null, completionDate: null, platformPlayedOn: null, timeToBeatMinutes: 999, coop: null, location: null
    });
    const result = toLibraryGame({ ...game, plays: [play('one', 60, 'COMPLETE'), play('two', 75, null)] });
    expect(result.totalMinutes).toBe(135);
    expect(result.status).toBe('completed');
  });
});

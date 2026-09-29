package com.bettergametracker.play;

import java.util.UUID;

public class PlayEntryNotFoundException extends RuntimeException {

    public PlayEntryNotFoundException(UUID gameId, UUID playEntryId) {
        super("Play entry not found for game " + gameId + ": " + playEntryId);
    }
}

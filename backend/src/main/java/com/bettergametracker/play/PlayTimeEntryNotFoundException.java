package com.bettergametracker.play;

import java.util.UUID;

public class PlayTimeEntryNotFoundException extends RuntimeException {

    public PlayTimeEntryNotFoundException(UUID playEntryId, UUID timeEntryId) {
        super("Play time entry not found for play entry " + playEntryId + ": " + timeEntryId);
    }
}

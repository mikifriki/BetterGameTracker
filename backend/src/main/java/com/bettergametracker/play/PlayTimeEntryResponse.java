package com.bettergametracker.play;

import java.time.LocalDate;
import java.util.UUID;

public record PlayTimeEntryResponse(UUID id, UUID playId, LocalDate date, int durationMinutes, String notes) {
}

package com.bettergametracker.play;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlayTimeEntryRequest(@NotNull LocalDate date, @Positive int durationMinutes, String notes) {
}

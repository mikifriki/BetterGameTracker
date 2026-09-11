package com.bettergametracker.play;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlayTimeEntryRequest(@NotNull LocalDate date, @Positive int durationMinutes, @Size(max = 255) String notes) {
}

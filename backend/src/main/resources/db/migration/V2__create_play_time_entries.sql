CREATE TABLE play_time_entries (
    id ${uuidType} NOT NULL PRIMARY KEY,
    play_entry_id ${uuidType} NOT NULL,
    date DATE NOT NULL,
    duration_minutes INTEGER NOT NULL,
    notes TEXT,
    CONSTRAINT chk_play_time_entries_duration_positive
        CHECK (duration_minutes > 0),
    CONSTRAINT fk_play_time_entries_play_entry
        FOREIGN KEY (play_entry_id)
        REFERENCES play_entries (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE INDEX idx_play_time_entries_play_entry_id ON play_time_entries (play_entry_id);

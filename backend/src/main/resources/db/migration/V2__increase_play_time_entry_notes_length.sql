-- Rebuild the table because SQLite cannot alter a column's declared type.
-- This migration also works on PostgreSQL and preserves all existing entries.
CREATE TABLE play_time_entries_expanded (
    id ${uuidType} NOT NULL PRIMARY KEY,
    play_entry_id ${uuidType} NOT NULL,
    date DATE NOT NULL,
    duration_minutes INTEGER NOT NULL,
    notes VARCHAR(5000),
    CONSTRAINT chk_play_time_entries_duration_positive
        CHECK (duration_minutes > 0),
    CONSTRAINT fk_play_time_entries_play_entry
        FOREIGN KEY (play_entry_id)
        REFERENCES play_entries (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

INSERT INTO play_time_entries_expanded (id, play_entry_id, date, duration_minutes, notes)
SELECT id, play_entry_id, date, duration_minutes, notes FROM play_time_entries;

DROP TABLE play_time_entries;
ALTER TABLE play_time_entries_expanded RENAME TO play_time_entries;
CREATE INDEX idx_play_time_entries_play_entry_id ON play_time_entries (play_entry_id);

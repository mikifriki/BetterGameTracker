CREATE TABLE application_users (
    id ${uuidType} NOT NULL PRIMARY KEY,
    google_subject VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE games (
    id ${uuidType} NOT NULL PRIMARY KEY,
    owner_id ${uuidType},
    game_title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    release_platform VARCHAR(255),
    release_date DATE,
    developer VARCHAR(255),
    meta_rating DECIMAL(3,1) CHECK (meta_rating BETWEEN 0 AND 10),
    user_rating DECIMAL(3,1) CHECK (user_rating BETWEEN 0 AND 10),
    physical_copy BOOLEAN,
    CONSTRAINT fk_games_owner
        FOREIGN KEY (owner_id)
        REFERENCES application_users (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE TABLE play_entries (
    id ${uuidType} NOT NULL PRIMARY KEY,
    playthrough_rating DECIMAL(3,1) CHECK (playthrough_rating BETWEEN 0 AND 10),
    completion_date DATE,
    platform_played_on VARCHAR(255),
    time_to_beat_minutes INTEGER CHECK (time_to_beat_minutes >= 0),
    completion_status VARCHAR(255) CHECK (completion_status IN ('IN_PROGRESS', 'COMPLETE', 'DID_NOT_FINISH')),
    coop BOOLEAN,
    location VARCHAR(255),
    game_id ${uuidType} NOT NULL,
    CONSTRAINT fk_play_entries_game
        FOREIGN KEY (game_id)
        REFERENCES games (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE TABLE reviews (
    id ${uuidType} NOT NULL PRIMARY KEY,
    review_date DATE,
    review_title VARCHAR(255),
    review_text VARCHAR(5000),
    rating DECIMAL(3,1) CHECK (rating BETWEEN 0 AND 10),
    play_entry_id ${uuidType} NOT NULL,
    CONSTRAINT fk_reviews_play_entry
        FOREIGN KEY (play_entry_id)
        REFERENCES play_entries (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE TABLE play_time_entries (
    id ${uuidType} NOT NULL PRIMARY KEY,
    play_entry_id ${uuidType} NOT NULL,
    date DATE NOT NULL,
    duration_minutes INTEGER NOT NULL,
    notes VARCHAR(255),
    CONSTRAINT chk_play_time_entries_duration_positive
        CHECK (duration_minutes > 0),
    CONSTRAINT fk_play_time_entries_play_entry
        FOREIGN KEY (play_entry_id)
        REFERENCES play_entries (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE INDEX idx_games_owner_id ON games (owner_id);
CREATE INDEX idx_play_entries_game_id ON play_entries (game_id);
CREATE INDEX idx_reviews_play_entry_id ON reviews (play_entry_id);
CREATE INDEX idx_play_time_entries_play_entry_id ON play_time_entries (play_entry_id);

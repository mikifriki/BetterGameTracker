CREATE TABLE application_users (
    id ${uuidType} NOT NULL PRIMARY KEY,
    google_subject VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE games (
    id ${uuidType} NOT NULL PRIMARY KEY,
    owner_id ${uuidType},
    game_title VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    release_platform VARCHAR(255),
    release_date VARCHAR(255),
    developer VARCHAR(255),
    meta_rating VARCHAR(255),
    user_rating VARCHAR(255),
    physical_copy VARCHAR(255),
    CONSTRAINT fk_games_owner
        FOREIGN KEY (owner_id)
        REFERENCES application_users (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE TABLE play_entries (
    id ${uuidType} NOT NULL PRIMARY KEY,
    playthrough_rating VARCHAR(255) NOT NULL,
    completion_date VARCHAR(255) NOT NULL,
    platform_played_on VARCHAR(255) NOT NULL,
    time_to_beat VARCHAR(255),
    completion_rate VARCHAR(255) NOT NULL,
    coop VARCHAR(255),
    location VARCHAR(255) NOT NULL,
    game_id ${uuidType} NOT NULL,
    CONSTRAINT fk_play_entries_game
        FOREIGN KEY (game_id)
        REFERENCES games (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE TABLE reviews (
    id ${uuidType} NOT NULL PRIMARY KEY,
    review_date VARCHAR(255),
    review_title VARCHAR(255),
    review_text VARCHAR(255),
    rating VARCHAR(255),
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

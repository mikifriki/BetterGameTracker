CREATE TABLE games (
    id ${uuidType} NOT NULL PRIMARY KEY,
    href_title TEXT,
    game_title TEXT NOT NULL,
    description TEXT NOT NULL,
    release_platform TEXT NOT NULL,
    release_date TEXT NOT NULL,
    developer TEXT NOT NULL,
    meta_rating TEXT NOT NULL,
    user_rating TEXT NOT NULL,
    physical_copy TEXT NOT NULL,
    cover_image TEXT NOT NULL
);

CREATE TABLE play_entries (
    id ${uuidType} NOT NULL PRIMARY KEY,
    legacy_id INTEGER,
    playthrough_rating TEXT NOT NULL,
    completion_date TEXT NOT NULL,
    platform_played_on TEXT NOT NULL,
    time_to_beat TEXT NOT NULL,
    completion_rate TEXT NOT NULL,
    coop TEXT,
    location TEXT NOT NULL,
    game_id ${uuidType} NOT NULL,
    CONSTRAINT fk_play_entries_game
        FOREIGN KEY (game_id)
        REFERENCES games (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE TABLE reviews (
    id ${uuidType} NOT NULL PRIMARY KEY,
    review_date TEXT,
    review_title TEXT,
    review_text TEXT,
    rating TEXT,
    play_entry_id ${uuidType} NOT NULL,
    CONSTRAINT fk_reviews_play_entry
        FOREIGN KEY (play_entry_id)
        REFERENCES play_entries (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
);

CREATE INDEX idx_play_entries_game_id ON play_entries (game_id);
CREATE INDEX idx_reviews_play_entry_id ON reviews (play_entry_id);

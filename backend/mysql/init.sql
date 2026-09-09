USE labelfollower;

-- Labels the app follows. `canonical_name` is the label's display name; matching
-- against Spotify's free-text `label` field on albums is handled in application
-- code (usecases/LabelIntrospector.kt / models/Label.kt), not here.
CREATE TABLE label (
    id INT PRIMARY KEY AUTO_INCREMENT,
    canonical_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (canonical_name)
);

-- A label accumulates copyright strings over time as different Spotify albums
-- report slightly different (but related) copyright text for the same label.
CREATE TABLE label_copyright (
    id INT PRIMARY KEY AUTO_INCREMENT,
    label_id INT NOT NULL,
    copyright_text VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    FOREIGN KEY (label_id) REFERENCES label(id),
    UNIQUE (label_id, copyright_text)
);

-- Tracks known from Spotify, identified by ISRC.
CREATE TABLE track (
    id INT PRIMARY KEY AUTO_INCREMENT,
    spotify_id VARCHAR(64) NOT NULL,
    isrc VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (spotify_id),
    UNIQUE (isrc)
);

-- Which tracks are already recorded as belonging to which label — this is what
-- LabelIntrospector diffs a label's live Spotify catalogue against to find
-- genuinely new releases.
CREATE TABLE label_track (
    label_id INT NOT NULL,
    track_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    PRIMARY KEY (label_id, track_id),
    FOREIGN KEY (label_id) REFERENCES label(id),
    FOREIGN KEY (track_id) REFERENCES track(id)
);

-- The Spotify account this app is permanently authenticated as (Authorization Code,
-- confidential client — backend/CLAUDE.md §7, integrations/spotify/SpotifyUserAuth.kt).
-- In practice a single row.
CREATE TABLE spotify_account (
    id INT PRIMARY KEY AUTO_INCREMENT,
    spotify_user_id VARCHAR(64) NOT NULL,
    refresh_token VARCHAR(512) NOT NULL,
    scopes VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (spotify_user_id)
);

CREATE TABLE profiles (
    telegram_user_id BIGINT PRIMARY KEY,
    username VARCHAR(64),
    display_name VARCHAR(80) NOT NULL,
    age SMALLINT NOT NULL CHECK (age >= 18 AND age <= 120),
    city VARCHAR(100) NOT NULL,
    bio VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE likes (
    from_user_id BIGINT NOT NULL REFERENCES profiles(telegram_user_id) ON DELETE CASCADE,
    to_user_id BIGINT NOT NULL REFERENCES profiles(telegram_user_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (from_user_id, to_user_id),
    CHECK (from_user_id <> to_user_id)
);

CREATE INDEX likes_to_user_id_idx ON likes (to_user_id);

CREATE TABLE matches (
    user_low_id BIGINT NOT NULL REFERENCES profiles(telegram_user_id) ON DELETE CASCADE,
    user_high_id BIGINT NOT NULL REFERENCES profiles(telegram_user_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_low_id, user_high_id),
    CHECK (user_low_id < user_high_id)
);

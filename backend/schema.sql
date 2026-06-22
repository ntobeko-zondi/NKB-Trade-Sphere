-- NKB Trade Sphere PostgreSQL schema for dgroup2729
-- The South African ID number is stored only as id_number_hash.


CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT UNIQUE,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT,
    full_name TEXT,
    phone TEXT,
    id_number_hash TEXT,
    average_rating REAL NOT NULL DEFAULT 0 CHECK (average_rating >= 0 AND average_rating <= 5),
    external_auth_id TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS user_id TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS id_number_hash TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS average_rating REAL NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS external_auth_id TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users DROP COLUMN IF EXISTS id_number;
UPDATE users SET user_id = email WHERE (user_id IS NULL OR user_id = '') AND email IS NOT NULL;

CREATE TABLE IF NOT EXISTS listings (
    listing_id BIGSERIAL PRIMARY KEY,
    seller_id TEXT NOT NULL,
    title TEXT NOT NULL,
    category TEXT NOT NULL,
    condition TEXT,
    description TEXT,
    price TEXT NOT NULL,
    image_url TEXT,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity >= 0),
    average_rating REAL NOT NULL DEFAULT 0 CHECK (average_rating >= 0 AND average_rating <= 5),
    status TEXT NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE listings ADD COLUMN IF NOT EXISTS seller_id TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS title TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS category TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS condition TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS price TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS image_url TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 1;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS average_rating REAL NOT NULL DEFAULT 0;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'active';
ALTER TABLE listings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS messages (
    message_id BIGSERIAL PRIMARY KEY,
    sender_id TEXT NOT NULL,
    receiver_id TEXT NOT NULL,
    listing_id BIGINT,
    message_text TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE messages ADD COLUMN IF NOT EXISTS sender_id TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS receiver_id TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS listing_id BIGINT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS message_text TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS saved_items (
    save_id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='saved_items' AND column_name='saved_listing_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema='public' AND table_name='saved_items' AND column_name='listing_id'
    ) THEN
        ALTER TABLE saved_items RENAME COLUMN saved_listing_id TO listing_id;
    END IF;
END $$;

ALTER TABLE saved_items ADD COLUMN IF NOT EXISTS user_id TEXT;
ALTER TABLE saved_items ADD COLUMN IF NOT EXISTS listing_id BIGINT;
ALTER TABLE saved_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS ratings (
    rating_id BIGSERIAL PRIMARY KEY,
    rater_id TEXT NOT NULL,
    rated_id TEXT NOT NULL,
    rating REAL NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rater_id TEXT;
ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rated_id TEXT;
ALTER TABLE ratings ADD COLUMN IF NOT EXISTS rating REAL;
ALTER TABLE ratings ADD COLUMN IF NOT EXISTS comment TEXT;
ALTER TABLE ratings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS listing_ratings (
    rating_id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL,
    buyer_id TEXT NOT NULL,
    rating REAL NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS listing_id BIGINT;
ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS buyer_id TEXT;
ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS rating REAL;
ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS comment TEXT;
ALTER TABLE listing_ratings ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_users_user_id_lower ON users (LOWER(user_id)) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_listings_seller_id ON listings(seller_id);
CREATE INDEX IF NOT EXISTS idx_listings_category ON listings(category);
CREATE INDEX IF NOT EXISTS idx_listings_status ON listings(status);
CREATE INDEX IF NOT EXISTS idx_messages_sender_id ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_receiver_id ON messages(receiver_id);
CREATE INDEX IF NOT EXISTS idx_messages_listing_id ON messages(listing_id);
CREATE INDEX IF NOT EXISTS idx_saved_items_user_id ON saved_items(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_items_listing_id ON saved_items(listing_id);
CREATE INDEX IF NOT EXISTS idx_ratings_rated_id ON ratings(rated_id);
CREATE INDEX IF NOT EXISTS idx_listing_ratings_listing_id ON listing_ratings(listing_id);

DELETE FROM saved_items a
USING saved_items b
WHERE a.ctid < b.ctid
  AND a.user_id = b.user_id
  AND a.listing_id = b.listing_id;
CREATE UNIQUE INDEX IF NOT EXISTS uq_saved_items_user_listing_idx ON saved_items(user_id, listing_id);

DELETE FROM listing_ratings a
USING listing_ratings b
WHERE a.ctid < b.ctid
  AND a.listing_id = b.listing_id
  AND a.buyer_id = b.buyer_id;
CREATE UNIQUE INDEX IF NOT EXISTS uq_listing_ratings_listing_buyer_idx ON listing_ratings(listing_id, buyer_id);

CREATE OR REPLACE FUNCTION sync_listing_status_from_quantity()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quantity IS NULL OR NEW.quantity <= 0 THEN
        NEW.quantity := 0;
        NEW.status := 'out_of_stock';
    ELSE
        NEW.status := 'active';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_listing_status_from_quantity ON listings;
CREATE TRIGGER trg_sync_listing_status_from_quantity
BEFORE INSERT OR UPDATE OF quantity ON listings
FOR EACH ROW
EXECUTE FUNCTION sync_listing_status_from_quantity();

CREATE OR REPLACE FUNCTION refresh_listing_average_rating()
RETURNS TRIGGER AS $$
DECLARE
    affected_listing BIGINT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        affected_listing := OLD.listing_id;
    ELSE
        affected_listing := NEW.listing_id;
    END IF;

    UPDATE listings
    SET average_rating = ROUND(COALESCE((SELECT AVG(rating) FROM listing_ratings WHERE listing_id = affected_listing), 0)::numeric, 1)
    WHERE listing_id = affected_listing;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_refresh_listing_average_rating ON listing_ratings;
CREATE TRIGGER trg_refresh_listing_average_rating
AFTER INSERT OR UPDATE OR DELETE ON listing_ratings
FOR EACH ROW
EXECUTE FUNCTION refresh_listing_average_rating();

CREATE OR REPLACE FUNCTION refresh_user_average_rating()
RETURNS TRIGGER AS $$
DECLARE
    affected_user TEXT;
BEGIN
    IF TG_OP = 'DELETE' THEN
        affected_user := OLD.rated_id;
    ELSE
        affected_user := NEW.rated_id;
    END IF;

    UPDATE users
    SET average_rating = ROUND(COALESCE((SELECT AVG(rating) FROM ratings WHERE rated_id = affected_user), 0)::numeric, 1)
    WHERE email = affected_user;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_refresh_user_average_rating ON ratings;
CREATE TRIGGER trg_refresh_user_average_rating
AFTER INSERT OR UPDATE OR DELETE ON ratings
FOR EACH ROW
EXECUTE FUNCTION refresh_user_average_rating();


-- Views for the DBF demo/report. These help show how the final ERD is implemented.
-- Drop existing views first because PostgreSQL cannot change a view column type using CREATE OR REPLACE VIEW.
DROP VIEW IF EXISTS v_listing_rating_summary CASCADE;
DROP VIEW IF EXISTS v_listing_details CASCADE;

CREATE OR REPLACE VIEW v_listing_details AS
SELECT l.listing_id,
       l.seller_id,
       COALESCE(u.full_name, l.seller_id) AS seller_name,
       l.title,
       l.category,
       l.condition,
       l.description,
       l.price,
       l.image_url,
       l.quantity,
       l.status,
       l.average_rating,
       l.created_at
FROM listings l
LEFT JOIN users u ON LOWER(u.email) = LOWER(l.seller_id);

CREATE OR REPLACE VIEW v_listing_rating_summary AS
SELECT l.listing_id,
       COUNT(r.rating_id) AS rating_count,
       ROUND(COALESCE(AVG(r.rating), 0)::numeric, 1) AS average_rating
FROM listings l
LEFT JOIN listing_ratings r ON r.listing_id = l.listing_id
GROUP BY l.listing_id;

-- Relationship constraints for a fresh database. These are wrapped so the script remains safe
-- if old test data already exists and needs to be cleaned first.
DO $$
BEGIN
    ALTER TABLE listings
        ADD CONSTRAINT fk_listings_seller
        FOREIGN KEY (seller_id) REFERENCES users(email) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object OR foreign_key_violation THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE saved_items
        ADD CONSTRAINT fk_saved_items_listing
        FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object OR foreign_key_violation THEN
    NULL;
END $$;

DO $$
BEGIN
    ALTER TABLE listing_ratings
        ADD CONSTRAINT fk_listing_ratings_listing
        FOREIGN KEY (listing_id) REFERENCES listings(listing_id) ON DELETE CASCADE;
EXCEPTION WHEN duplicate_object OR foreign_key_violation THEN
    NULL;
END $$;

-- Drop old strict constraints on profiles
ALTER TABLE profiles DROP CONSTRAINT IF EXISTS profiles_email_key;

ALTER TABLE profiles DROP CONSTRAINT IF EXISTS profiles_username_key;

-- Drop old partial indexes if they exist
DROP INDEX IF EXISTS idx_profiles_unique_active_email;

DROP INDEX IF EXISTS idx_profiles_unique_active_username;

-- Create partial unique indexes checking for active users (deleted_at IS NULL)
CREATE UNIQUE INDEX idx_profiles_unique_active_email ON profiles (email)
WHERE
    deleted_at IS NULL;

CREATE UNIQUE INDEX idx_profiles_unique_active_username ON profiles (username)
WHERE
    deleted_at IS NULL;
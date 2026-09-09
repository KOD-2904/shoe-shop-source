-- Manual migration for the new auth model.
-- Review constraint names in your database before running on non-local data.

ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE user_account
    MODIFY COLUMN password VARCHAR(255) NULL;

ALTER TABLE user_account
    MODIFY COLUMN email VARCHAR(255) NOT NULL;

ALTER TABLE user_account
    MODIFY COLUMN phone VARCHAR(20) NULL;

ALTER TABLE user_account
    DROP COLUMN IF EXISTS username,
    DROP COLUMN IF EXISTS firstname,
    DROP COLUMN IF EXISTS lastname;

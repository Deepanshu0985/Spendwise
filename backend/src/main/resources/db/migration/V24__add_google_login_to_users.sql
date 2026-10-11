-- "Sign in with Google" (D-19). A person who only ever signs in with Google has no password, so the hash becomes
-- optional; google_subject is Google's stable account id (the token's "sub"), which - unlike an email address - never
-- changes or gets reused, so it is what a returning Google user is matched on. Each Google account maps to one user.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255);
CREATE UNIQUE INDEX uk_users_google_subject ON users (google_subject) WHERE google_subject IS NOT NULL;

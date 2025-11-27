-- HyperTube User Management Schema
-- Version 1: Initial schema for users, OAuth accounts, and password reset tokens

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table: Core user information and authentication
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60), -- bcrypt hash (nullable for OAuth-only users)
    profile_picture_url VARCHAR(500),
    preferred_language VARCHAR(5) DEFAULT 'en' NOT NULL,
    is_email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT username_length CHECK (char_length(username) >= 3),
    CONSTRAINT username_format CHECK (username ~ '^[a-zA-Z0-9_]+$'),
    CONSTRAINT email_format CHECK (email ~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT preferred_language_format CHECK (preferred_language ~ '^[a-z]{2}(-[A-Z]{2})?$')
);

-- Indexes for performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_last_login_at ON users(last_login_at DESC) WHERE last_login_at IS NOT NULL;

-- Comment on table
COMMENT ON TABLE users IS 'Core user accounts with authentication credentials';
COMMENT ON COLUMN users.password_hash IS 'bcrypt hash with salt (cost factor: 12) - nullable for OAuth-only users';
COMMENT ON COLUMN users.preferred_language IS 'ISO 639-1 language code (e.g., en, fr, es) or with region (e.g., en-US)';

-- OAuth accounts table: External authentication providers
CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    access_token TEXT, -- Encrypted in application layer
    refresh_token TEXT, -- Encrypted in application layer
    token_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT oauth_provider_check CHECK (provider IN ('42', 'google', 'github')),
    CONSTRAINT oauth_unique_account UNIQUE (provider, provider_user_id)
);

-- Indexes for performance
CREATE INDEX idx_oauth_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_oauth_provider_user ON oauth_accounts(provider, provider_user_id);

-- Comment on table
COMMENT ON TABLE oauth_accounts IS 'OAuth2 authentication accounts linked to users';
COMMENT ON COLUMN oauth_accounts.provider IS 'OAuth provider: 42 (mandatory), google, github (optional)';
COMMENT ON COLUMN oauth_accounts.access_token IS 'OAuth access token - encrypted at application layer before storage';
COMMENT ON COLUMN oauth_accounts.refresh_token IS 'OAuth refresh token - encrypted at application layer before storage';

-- Password reset tokens table: For forgot password flow
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE, -- SHA-256 hash of the token
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Constraints
    CONSTRAINT token_not_expired CHECK (used_at IS NULL OR used_at < expires_at)
);

-- Indexes for performance
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_token_hash ON password_reset_tokens(token_hash) WHERE used_at IS NULL;
CREATE INDEX idx_password_reset_expires_at ON password_reset_tokens(expires_at) WHERE used_at IS NULL;

-- Comment on table
COMMENT ON TABLE password_reset_tokens IS 'One-time tokens for password reset flow (15-minute TTL)';
COMMENT ON COLUMN password_reset_tokens.token_hash IS 'SHA-256 hash of the reset token sent to user email';
COMMENT ON COLUMN password_reset_tokens.used_at IS 'Timestamp when token was used (NULL if unused)';

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_oauth_accounts_updated_at
    BEFORE UPDATE ON oauth_accounts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Clean up expired password reset tokens (run periodically via scheduled job)
CREATE OR REPLACE FUNCTION cleanup_expired_password_reset_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM password_reset_tokens
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_password_reset_tokens() IS 'Deletes password reset tokens older than 7 days past expiration';

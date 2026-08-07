-- Extend password_reset_tokens for email OTP flow:
-- 1) forgot-password stores otp_hash (token NULL)
-- 2) verify-otp issues opaque reset token into token column
-- 3) reset-password consumes token and marks used

ALTER TABLE password_reset_tokens
    ALTER COLUMN token DROP NOT NULL;

ALTER TABLE password_reset_tokens
    ADD COLUMN otp_hash VARCHAR(255),
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_password_reset_token ON password_reset_tokens (token)
    WHERE token IS NOT NULL;

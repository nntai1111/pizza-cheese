-- name: findByToken
SELECT id, user_id, token, otp_hash, expires_at, used, attempts, created_at
FROM password_reset_tokens
WHERE token = :token

-- name: findLatestByUserId
SELECT id, user_id, token, otp_hash, expires_at, used, attempts, created_at
FROM password_reset_tokens
WHERE user_id = :userId
ORDER BY created_at DESC
LIMIT 1

-- name: insert
INSERT INTO password_reset_tokens (id, user_id, token, otp_hash, expires_at, used, attempts, created_at)
VALUES (:id, :userId, :token, :otpHash, :expiresAt, :used, :attempts, :createdAt)

-- name: updateAfterOtpVerified
UPDATE password_reset_tokens
SET token = :token,
    expires_at = :expiresAt,
    attempts = :attempts
WHERE id = :id

-- name: incrementAttempts
UPDATE password_reset_tokens
SET attempts = attempts + 1
WHERE id = :id

-- name: markUsed
UPDATE password_reset_tokens
SET used = TRUE
WHERE id = :id

-- name: deleteById
DELETE FROM password_reset_tokens WHERE id = :id

-- name: deleteByUserId
DELETE FROM password_reset_tokens WHERE user_id = :userId

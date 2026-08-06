-- name: findByToken
SELECT id, token, user_id, expires_at, created_at
FROM email_verification_tokens
WHERE token = :token

-- name: findLatestByUserId
SELECT id, token, user_id, expires_at, created_at
FROM email_verification_tokens
WHERE user_id = :userId
ORDER BY created_at DESC
LIMIT 1

-- name: insert
INSERT INTO email_verification_tokens (id, token, user_id, expires_at, created_at)
VALUES (:id, :token, :userId, :expiresAt, :createdAt)

-- name: deleteById
DELETE FROM email_verification_tokens WHERE id = :id

-- name: deleteByUserId
DELETE FROM email_verification_tokens WHERE user_id = :userId
